package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.CoupleBindDTO;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.CoupleService;
import com.love.couplelife.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 情侣关系服务实现：通过邀请码进行双向绑定，以及统一解绑。
 *
 * <p>协作组件：
 * <ul>
 *     <li>{@link UserMapper}：根据邀请码定位伴侣账号</li>
 *     <li>{@link CoupleRelationMapper}：维护双向情侣关系记录（每人一行，共享同一 coupleId）</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>邀请码大小写不敏感，统一转大写匹配</li>
 *     <li>不允许绑定自己；任何一方已存在生效绑定都会拒绝</li>
 *     <li>双方各持一行 CoupleRelation 记录，但 coupleId 共用（取我侧记录主键作为共享 id）</li>
 *     <li>如历史上存在过相同 (user_id, partner_id) 的记录则复用并更新，避免唯一索引冲突</li>
 *     <li>解绑为软解绑（status=0），按 coupleId 一并失效双方记录</li>
 * </ul>
 */
@Service
public class CoupleServiceImpl implements CoupleService {
    private final UserMapper userMapper;
    private final CoupleRelationMapper coupleRelationMapper;

    public CoupleServiceImpl(UserMapper userMapper, CoupleRelationMapper coupleRelationMapper) {
        this.userMapper = userMapper;
        this.coupleRelationMapper = coupleRelationMapper;
    }

    /**
     * 通过邀请码绑定情侣关系（双向插入或复用历史记录）。
     *
     * <p>事务步骤：
     * <ol>
     *     <li>根据邀请码定位 partner，校验非空、非自己、双方均未已绑定</li>
     *     <li>处理"我 → ta"方向：历史上若有同向记录则复用、否则插入</li>
     *     <li>取我侧记录主键作为共享 coupleId，回填到双方</li>
     *     <li>处理"ta → 我"方向：同样复用或插入，写入相同 coupleId</li>
     * </ol>
     *
     * <p>并发安全：通过 {@code SELECT ... FOR UPDATE} 悲观锁防止两个并发请求
     * 同时通过存在性校验、导致同一用户被绑定给多个伴侣的竞态问题。
     * 锁定顺序始终按 userId 升序，避免死锁。
     *
     * @param dto 包含邀请码与可选的恋爱起始日期（缺省取今天）
     * @throws BizException 邀请码无效、试图绑定自己、任一方已被绑定
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(CoupleBindDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        User partner = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getInviteCode, dto.getInviteCode().trim().toUpperCase())
                .last("limit 1"));
        if (partner == null) {
            throw new BizException("邀请码无效");
        }
        if (partner.getId().equals(userId)) {
            throw new BizException("不能绑定自己哦");
        }
        // 按 ID 升序锁定双方行，防止并发绑定导致死锁
        Long first = Math.min(userId, partner.getId());
        Long second = Math.max(userId, partner.getId());
        lockForUpdate(first);
        lockForUpdate(second);
        if (existsRelation(userId)) {
            throw new BizException("你已经绑定了情侣");
        }
        if (existsRelation(partner.getId())) {
            throw new BizException("对方已经被绑定啦");
        }
        LocalDate startDate = dto.getLoveStartDate() == null ? LocalDate.now() : dto.getLoveStartDate();
        // 先处理我侧记录：若历史已有同 user_id+partner_id 行，则复用更新；否则插入
        CoupleRelation mine = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getPartnerId, partner.getId())
                .last("limit 1"));
        if (mine == null) {
            mine = new CoupleRelation();
            mine.setUserId(userId);
            mine.setPartnerId(partner.getId());
            mine.setLoveStartDate(startDate);
            mine.setStatus(1);
            coupleRelationMapper.insert(mine);
        }
        Long sharedCoupleId = mine.getId();
        mine.setCoupleId(sharedCoupleId);
        mine.setLoveStartDate(startDate);
        mine.setStatus(1);
        coupleRelationMapper.updateById(mine);

        CoupleRelation his = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, partner.getId())
                .eq(CoupleRelation::getPartnerId, userId)
                .last("limit 1"));
        if (his == null) {
            his = new CoupleRelation();
            his.setCoupleId(sharedCoupleId);
            his.setUserId(partner.getId());
            his.setPartnerId(userId);
            his.setLoveStartDate(startDate);
            his.setStatus(1);
            coupleRelationMapper.insert(his);
        } else {
            his.setCoupleId(sharedCoupleId);
            his.setLoveStartDate(startDate);
            his.setStatus(1);
            coupleRelationMapper.updateById(his);
        }
    }

    /**
     * 解除当前用户的情侣关系（软删除：status=0）。
     *
     * <p>未绑定时静默返回（幂等）；解绑会按共享 coupleId 同步失效双方记录。
     */
    @Override
    public void unbind() {
        Long userId = SecurityUtil.currentUserId();
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation == null) {
            return;
        }
        coupleRelationMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CoupleRelation>()
                .eq(CoupleRelation::getCoupleId, relation.getCoupleId())
                .set(CoupleRelation::getStatus, 0));
    }

    /** 判断指定用户是否已存在生效的情侣关系。 */
    private boolean existsRelation(Long userId) {
        return coupleRelationMapper.exists(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1));
    }

    /**
     * 对指定用户的生效情侣关系行加悲观锁（{@code SELECT ... FOR UPDATE}）。
     * <p>若该用户当前无生效记录，则加在 user_id 维度的全表扫描上（MySQL 会对扫描到的行加 next-key lock），
     * 从而阻止并发事务向同一 user_id 插入新的 status=1 记录。</p>
     */
    private void lockForUpdate(Long userId) {
        coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("FOR UPDATE"));
    }
}
