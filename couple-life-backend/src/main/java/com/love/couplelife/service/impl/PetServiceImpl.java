package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.PetInteractionDTO;
import com.love.couplelife.dto.PetSelectionRequestDTO;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.Pet;
import com.love.couplelife.entity.PetInteractionLog;
import com.love.couplelife.entity.PetSelectionRequest;
import com.love.couplelife.entity.PetType;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.PetInteractionLogMapper;
import com.love.couplelife.mapper.PetMapper;
import com.love.couplelife.mapper.PetSelectionRequestMapper;
import com.love.couplelife.mapper.PetTypeMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.PetService;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.PetSelectionRequestVO;
import com.love.couplelife.vo.PetTypeVO;
import com.love.couplelife.vo.PetVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 电子宠物业务实现。
 *
 * <p>核心规则（MVP 阶段）：
 * <ul>
 *     <li><b>属于情侣</b>：所有宠物数据按 {@code couple_id} 共享，未绑定情侣的用户无法使用。</li>
 *     <li><b>共同同意</b>：选择 / 更换宠物需对方同意，超时 24h 自动过期。</li>
 *     <li><b>互动收益</b>（写死的简化数值，便于前期手感调试）：
 *         <ul>
 *             <li>FEED：+intimacy 5、+fullness 25</li>
 *             <li>PET ：+intimacy 3、+mood 15（每日上限 5 次）</li>
 *             <li>PLAY：+intimacy 4、+mood 10、-fullness 5</li>
 *         </ul>
 *     </li>
 *     <li><b>等级公式</b>：level = 1 + intimacy/100；stage：&lt;500 BABY，&lt;2000 TEEN，否则 ADULT。</li>
 *     <li><b>每日衰减</b>：fullness -10、mood -8（不低于 0），按 {@code last_decay_date} 幂等。</li>
 * </ul>
 */
@Service
public class PetServiceImpl implements PetService {

    /** 选择请求默认有效期（小时）。 */
    private static final int SELECTION_EXPIRE_HOURS = 24;
    /** 抚摸每日次数上限（个人维度）。 */
    private static final int PET_DAILY_LIMIT = 5;

    private final PetMapper petMapper;
    private final PetTypeMapper petTypeMapper;
    private final PetInteractionLogMapper logMapper;
    private final PetSelectionRequestMapper selectionMapper;
    private final CoupleRelationMapper coupleRelationMapper;
    private final UserMapper userMapper;

    public PetServiceImpl(PetMapper petMapper,
                          PetTypeMapper petTypeMapper,
                          PetInteractionLogMapper logMapper,
                          PetSelectionRequestMapper selectionMapper,
                          CoupleRelationMapper coupleRelationMapper,
                          UserMapper userMapper) {
        this.petMapper = petMapper;
        this.petTypeMapper = petTypeMapper;
        this.logMapper = logMapper;
        this.selectionMapper = selectionMapper;
        this.coupleRelationMapper = coupleRelationMapper;
        this.userMapper = userMapper;
    }

    // ===================== 查询接口 =====================

    @Override
    public List<PetTypeVO> listTypes() {
        return petTypeMapper.selectList(new LambdaQueryWrapper<PetType>()
                        .eq(PetType::getEnabled, 1)
                        .orderByAsc(PetType::getSortOrder))
                .stream().map(this::toTypeVO).toList();
    }

    @Override
    public PetVO currentPet() {
        Long userId = SecurityUtil.currentUserId();
        Long coupleId = findCoupleId(userId);
        if (coupleId == null) {
            return null;
        }
        Pet pet = findActivePet(coupleId);
        return pet == null ? null : toPetVO(pet);
    }

    // ===================== 选择请求 =====================

    /**
     * 发起宠物选择 / 更换请求。
     *
     * <p>校验：必须已绑定情侣；同一情侣不允许存在多个 PENDING 请求（先取消旧的再发新的）。</p>
     */
    @Override
    @Transactional
    public PetSelectionRequestVO createSelectionRequest(PetSelectionRequestDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        CoupleRelation relation = requireRelation(userId);

        PetType type = petTypeMapper.selectById(dto.getPetTypeId());
        if (type == null || type.getEnabled() == null || type.getEnabled() == 0) {
            throw new BizException("所选宠物种类不存在或已下架");
        }

        // 同一情侣同一时刻只允许一个 PENDING；如果已存在则将其置为 EXPIRED 让位给新请求
        selectionMapper.update(null, new LambdaUpdateWrapper<PetSelectionRequest>()
                .eq(PetSelectionRequest::getCoupleId, relation.getCoupleId())
                .eq(PetSelectionRequest::getStatus, "PENDING")
                .set(PetSelectionRequest::getStatus, "EXPIRED"));

        PetSelectionRequest req = new PetSelectionRequest();
        req.setCoupleId(relation.getCoupleId());
        req.setRequesterId(userId);
        req.setPartnerId(relation.getPartnerId());
        req.setPetTypeId(dto.getPetTypeId());
        req.setNickname(dto.getNickname());
        req.setStatus("PENDING");
        req.setExpireTime(LocalDateTime.now().plusHours(SELECTION_EXPIRE_HOURS));
        selectionMapper.insert(req);
        return toSelectionVO(req);
    }

    @Override
    public List<PetSelectionRequestVO> listSelectionRequests() {
        Long userId = SecurityUtil.currentUserId();
        // 顺手清理过期请求，保证返回结果状态准确
        expireOverdueRequests();
        List<PetSelectionRequest> list = selectionMapper.selectList(new LambdaQueryWrapper<PetSelectionRequest>()
                .and(w -> w.eq(PetSelectionRequest::getRequesterId, userId)
                        .or().eq(PetSelectionRequest::getPartnerId, userId))
                .orderByDesc(PetSelectionRequest::getCreateTime));
        return list.stream().map(this::toSelectionVO).toList();
    }

    @Override
    @Transactional
    public PetVO agreeSelectionRequest(Long requestId) {
        Long userId = SecurityUtil.currentUserId();
        PetSelectionRequest req = requireMyPendingRequest(requestId, userId);

        // 标记当前请求为已同意
        req.setStatus("AGREED");
        req.setDecidedTime(LocalDateTime.now());
        selectionMapper.updateById(req);

        // 将既有活跃宠物置为 0（被替换）
        petMapper.update(null, new LambdaUpdateWrapper<Pet>()
                .eq(Pet::getCoupleId, req.getCoupleId())
                .eq(Pet::getStatus, 1)
                .set(Pet::getStatus, 0));

        Pet pet = new Pet();
        pet.setCoupleId(req.getCoupleId());
        pet.setPetTypeId(req.getPetTypeId());
        pet.setNickname(req.getNickname());
        pet.setIntimacy(0);
        pet.setFullness(80);
        pet.setMood(80);
        pet.setLevel(1);
        pet.setStage("BABY");
        pet.setBoundDate(LocalDate.now());
        pet.setLastDecayDate(LocalDate.now());
        pet.setStatus(1);
        petMapper.insert(pet);
        return toPetVO(pet);
    }

    @Override
    public void rejectSelectionRequest(Long requestId) {
        Long userId = SecurityUtil.currentUserId();
        PetSelectionRequest req = requireMyPendingRequest(requestId, userId);
        req.setStatus("REJECTED");
        req.setDecidedTime(LocalDateTime.now());
        selectionMapper.updateById(req);
    }

    // ===================== 互动 =====================

    /**
     * 互动行为：先做属性衰减结算，再按 action 累加属性 / 亲密度，并写互动日志。
     *
     * <p>注意：互动结果对情侣双方共享 —— 任一方互动均累计到同一只宠物上。</p>
     */
    @Override
    @Transactional
    public PetVO interact(PetInteractionDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        CoupleRelation relation = requireRelation(userId);
        Pet pet = findActivePet(relation.getCoupleId());
        if (pet == null) {
            throw new BizException("当前还没有共同的宠物，请先共同选择");
        }

        // 进入互动前先按当前日期补齐衰减，避免长时间不上线后属性反而虚高
        applyDecayInPlace(pet);

        int intimacyDelta = 0;
        int fullnessDelta = 0;
        int moodDelta = 0;
        String action = dto.getAction() == null ? "" : dto.getAction().trim().toUpperCase();
        switch (action) {
            case "FEED" -> {
                if (pet.getFullness() != null && pet.getFullness() >= 100) {
                    throw new BizException("宠物已经吃饱啦，过会儿再来吧");
                }
                intimacyDelta = 5;
                fullnessDelta = 25;
            }
            case "PET" -> {
                // 抚摸有每日次数限制（按当前操作人维度）
                long todayCount = countTodayInteractions(userId, "PET");
                if (todayCount >= PET_DAILY_LIMIT) {
                    throw new BizException("今天已经摸够 " + PET_DAILY_LIMIT + " 次啦，明天再来吧");
                }
                intimacyDelta = 3;
                moodDelta = 15;
            }
            case "PLAY" -> {
                intimacyDelta = 4;
                moodDelta = 10;
                fullnessDelta = -5;
            }
            default -> throw new BizException("未知的互动类型：" + action);
        }

        pet.setIntimacy(clampNonNeg((pet.getIntimacy() == null ? 0 : pet.getIntimacy()) + intimacyDelta));
        pet.setFullness(clamp0to100((pet.getFullness() == null ? 0 : pet.getFullness()) + fullnessDelta));
        pet.setMood(clamp0to100((pet.getMood() == null ? 0 : pet.getMood()) + moodDelta));
        recomputeGrowth(pet);
        petMapper.updateById(pet);

        PetInteractionLog log = new PetInteractionLog();
        log.setPetId(pet.getId());
        log.setCoupleId(pet.getCoupleId());
        log.setUserId(userId);
        log.setAction(action);
        log.setIntimacyDelta(intimacyDelta);
        log.setFullnessDelta(fullnessDelta);
        log.setMoodDelta(moodDelta);
        logMapper.insert(log);

        return toPetVO(pet);
    }

    // ===================== 定时任务调用入口 =====================

    @Override
    public void runDailyDecay() {
        // 仅扫描活跃宠物；逐只处理保证错一只不影响整体
        List<Pet> activePets = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getStatus, 1));
        for (Pet pet : activePets) {
            try {
                applyDecayInPlace(pet);
                petMapper.updateById(pet);
            } catch (Exception ignore) {
                // 单只失败不影响其他宠物的衰减
            }
        }
    }

    @Override
    public void expireOverdueRequests() {
        selectionMapper.update(null, new LambdaUpdateWrapper<PetSelectionRequest>()
                .eq(PetSelectionRequest::getStatus, "PENDING")
                .lt(PetSelectionRequest::getExpireTime, LocalDateTime.now())
                .set(PetSelectionRequest::getStatus, "EXPIRED"));
    }

    // ===================== 私有工具 =====================

    /**
     * 按"自上次衰减日 → 今日"应用每日衰减；幂等。
     * <p>两次衰减之间最多累积 30 天，避免极端情况下数值大幅塌方。</p>
     */
    private void applyDecayInPlace(Pet pet) {
        LocalDate today = LocalDate.now();
        LocalDate last = pet.getLastDecayDate() == null ? pet.getBoundDate() : pet.getLastDecayDate();
        if (last == null) {
            last = today;
        }
        long days = ChronoUnit.DAYS.between(last, today);
        if (days <= 0) {
            return;
        }
        if (days > 30) {
            days = 30;
        }
        int fullness = pet.getFullness() == null ? 0 : pet.getFullness();
        int mood = pet.getMood() == null ? 0 : pet.getMood();
        fullness = clamp0to100(fullness - (int) days * 10);
        mood = clamp0to100(mood - (int) days * 8);
        pet.setFullness(fullness);
        pet.setMood(mood);
        pet.setLastDecayDate(today);
    }

    /** 根据亲密度刷新等级与成长阶段。 */
    private void recomputeGrowth(Pet pet) {
        int intimacy = pet.getIntimacy() == null ? 0 : pet.getIntimacy();
        pet.setLevel(1 + intimacy / 100);
        if (intimacy < 500) {
            pet.setStage("BABY");
        } else if (intimacy < 2000) {
            pet.setStage("TEEN");
        } else {
            pet.setStage("ADULT");
        }
    }

    private int clamp0to100(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private int clampNonNeg(int v) {
        return Math.max(0, v);
    }

    /** 统计指定用户当日某种互动的次数。 */
    private long countTodayInteractions(Long userId, String action) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return logMapper.selectCount(new LambdaQueryWrapper<PetInteractionLog>()
                .eq(PetInteractionLog::getUserId, userId)
                .eq(PetInteractionLog::getAction, action)
                .ge(PetInteractionLog::getCreateTime, start)
                .lt(PetInteractionLog::getCreateTime, end));
    }

    /** 必须存在生效的情侣关系，否则抛业务异常。 */
    private CoupleRelation requireRelation(Long userId) {
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation == null || relation.getCoupleId() == null) {
            throw new BizException("请先绑定情侣关系");
        }
        return relation;
    }

    /** 取当前用户已生效情侣的 coupleId（未绑定则返回 null，不抛异常，用于 currentPet 这种"软"路径）。 */
    private Long findCoupleId(Long userId) {
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        return relation == null ? null : relation.getCoupleId();
    }

    /** 取一对情侣的活跃宠物。 */
    private Pet findActivePet(Long coupleId) {
        return petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getCoupleId, coupleId)
                .eq(Pet::getStatus, 1)
                .last("limit 1"));
    }

    /** 校验 requestId 是当前用户作为 partner 待处理的 PENDING 请求。 */
    private PetSelectionRequest requireMyPendingRequest(Long requestId, Long userId) {
        PetSelectionRequest req = selectionMapper.selectById(requestId);
        if (req == null) {
            throw new BizException("请求不存在");
        }
        if (!userId.equals(req.getPartnerId())) {
            throw new BizException("无权处理该请求");
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new BizException("请求当前状态不可处理");
        }
        if (req.getExpireTime() != null && req.getExpireTime().isBefore(LocalDateTime.now())) {
            // 兜底：已过期但状态尚未刷新
            req.setStatus("EXPIRED");
            selectionMapper.updateById(req);
            throw new BizException("请求已过期");
        }
        return req;
    }

    private PetTypeVO toTypeVO(PetType t) {
        PetTypeVO vo = new PetTypeVO();
        vo.setId(t.getId());
        vo.setCode(t.getCode());
        vo.setName(t.getName());
        vo.setDescription(t.getDescription());
        vo.setAvatar(t.getAvatar());
        vo.setSpriteUrl(t.getSpriteUrl());
        vo.setSortOrder(t.getSortOrder());
        return vo;
    }

    private PetVO toPetVO(Pet pet) {
        PetVO vo = new PetVO();
        vo.setId(pet.getId());
        vo.setCoupleId(pet.getCoupleId());
        vo.setPetTypeId(pet.getPetTypeId());
        vo.setNickname(pet.getNickname());
        vo.setIntimacy(pet.getIntimacy());
        vo.setFullness(pet.getFullness());
        vo.setMood(pet.getMood());
        vo.setLevel(pet.getLevel());
        vo.setStage(pet.getStage());
        vo.setBoundDate(pet.getBoundDate());
        // 陪伴天数：今日 - 绑定日 + 1（含今日）
        if (pet.getBoundDate() != null) {
            vo.setCompanionDays(ChronoUnit.DAYS.between(pet.getBoundDate(), LocalDate.now()) + 1);
        }
        PetType type = petTypeMapper.selectById(pet.getPetTypeId());
        if (type != null) {
            vo.setPetTypeCode(type.getCode());
            vo.setPetTypeName(type.getName());
            vo.setSpriteUrl(type.getSpriteUrl());
            vo.setTypeAvatar(type.getAvatar());
        }
        return vo;
    }

    private PetSelectionRequestVO toSelectionVO(PetSelectionRequest req) {
        PetSelectionRequestVO vo = new PetSelectionRequestVO();
        vo.setId(req.getId());
        vo.setCoupleId(req.getCoupleId());
        vo.setRequesterId(req.getRequesterId());
        vo.setPartnerId(req.getPartnerId());
        vo.setPetTypeId(req.getPetTypeId());
        vo.setNickname(req.getNickname());
        vo.setStatus(req.getStatus());
        vo.setExpireTime(req.getExpireTime());
        vo.setCreateTime(req.getCreateTime());
        User requester = userMapper.selectById(req.getRequesterId());
        vo.setRequesterNickname(requester == null ? null : requester.getNickname());
        PetType type = petTypeMapper.selectById(req.getPetTypeId());
        if (type != null) {
            vo.setPetTypeName(type.getName());
            vo.setPetTypeCode(type.getCode());
        }
        return vo;
    }
}
