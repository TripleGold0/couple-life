package com.love.couplelife.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.dto.CheckinDTO;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.DailyCheckin;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.DailyCheckinMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.CheckinService;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.CheckinVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 每日打卡服务实现。
 *
 * <p>职责：用户每天发布心情打卡（心情 emoji + 文字），同情侣双方可互相查看日历与逐日详情。
 * <p>协作组件：
 * <ul>
 *     <li>{@link DailyCheckinMapper}：打卡记录表，按 (userId, checkinDate) 唯一</li>
 *     <li>{@link CoupleRelationMapper}：找出当前用户的另一半 userId、couple_id</li>
 *     <li>{@link UserMapper}：在打卡 VO 中带出昵称</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>同一天同一用户的打卡为 upsert（已存在则更新，不存在则新建）</li>
 *     <li>未绑定情侣也允许打卡：先打卡后绑定的历史记录会在再次打卡时回填 coupleId</li>
 *     <li>日历/逐日查询会聚合双方（自身 + partner）的打卡数据</li>
 * </ul>
 */
@Service
public class CheckinServiceImpl implements CheckinService {
    private final DailyCheckinMapper checkinMapper;
    private final CoupleRelationMapper coupleRelationMapper;
    private final UserMapper userMapper;

    public CheckinServiceImpl(DailyCheckinMapper checkinMapper, CoupleRelationMapper coupleRelationMapper, UserMapper userMapper) {
        this.checkinMapper = checkinMapper;
        this.coupleRelationMapper = coupleRelationMapper;
        this.userMapper = userMapper;
    }

    /**
     * 新增或更新当日打卡（按 userId + checkinDate upsert）。
     *
     * <p>若当天已存在打卡：更新心情/文字内容；如果原记录 coupleId 为空（用户绑定情侣前打的卡），
     * 在此再次刷新一次 coupleId，确保未来按情侣维度查询能命中。
     *
     * @param dto 打卡数据：日期、心情 emoji、心情文字、内容
     * @return 该打卡记录的主键 id
     */
    @Override
    public Map<String, Long> add(CheckinDTO dto) {
        Long userId = SecurityUtil.currentUserId();
        DailyCheckin existing = checkinMapper.selectOne(new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, dto.getCheckinDate())
                .last("limit 1"));
        if (existing != null) {
            // 修复：先打卡后绑定情侣的场景，update 分支需刷新 coupleId
            if (existing.getCoupleId() == null) {
                existing.setCoupleId(findCoupleId(userId));
            }
            existing.setMoodEmoji(dto.getMoodEmoji());
            existing.setMoodText(dto.getMoodText());
            existing.setContent(dto.getContent());
            checkinMapper.updateById(existing);
            return Map.of("id", existing.getId());
        }
        DailyCheckin checkin = new DailyCheckin();
        checkin.setUserId(userId);
        checkin.setCoupleId(findCoupleId(userId));
        checkin.setCheckinDate(dto.getCheckinDate());
        checkin.setMoodEmoji(dto.getMoodEmoji());
        checkin.setMoodText(dto.getMoodText());
        checkin.setContent(dto.getContent());
        checkinMapper.insert(checkin);
        return Map.of("id", checkin.getId());
    }

    /**
     * 查询指定月份内自己 + 伴侣的全部打卡，用于前端日历视图。
     *
     * @param month 目标年月
     * @return 当月所有打卡 VO，按日期升序
     */
    @Override
    public List<CheckinVO> calendar(YearMonth month) {
        Long userId = SecurityUtil.currentUserId();
        List<Long> userIds = coupleUserIds(userId);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        return checkinMapper.selectList(new LambdaQueryWrapper<DailyCheckin>()
                        .in(DailyCheckin::getUserId, userIds)
                        .between(DailyCheckin::getCheckinDate, start, end)
                        .orderByAsc(DailyCheckin::getCheckinDate))
                .stream().map(this::toVO).toList();
    }

    /**
     * 查询情侣双方某一天的打卡详情（点击日历某天时调用）。
     *
     * @param date 指定日期
     * @return 当天双方的打卡 VO 列表（最多 2 条）
     */
    @Override
    public List<CheckinVO> coupleByDate(LocalDate date) {
        Long userId = SecurityUtil.currentUserId();
        return checkinMapper.selectList(new LambdaQueryWrapper<DailyCheckin>()
                        .in(DailyCheckin::getUserId, coupleUserIds(userId))
                        .eq(DailyCheckin::getCheckinDate, date))
                .stream().map(this::toVO).toList();
    }

    /** 返回当前用户 + 已绑定伴侣 的 userId 列表（未绑定时仅含自身）。 */
    private List<Long> coupleUserIds(Long userId) {
        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);
        CoupleRelation relation = findRelation(userId);
        if (relation != null) {
            userIds.add(relation.getPartnerId());
        }
        return userIds;
    }

    /** 取当前用户已生效的 coupleId，未绑定返回 null（打卡场景下允许 null）。 */
    private Long findCoupleId(Long userId) {
        CoupleRelation relation = findRelation(userId);
        return relation == null ? null : relation.getCoupleId();
    }

    /** 查询用户当前生效（status=1）的情侣关系记录，无则返回 null。 */
    private CoupleRelation findRelation(Long userId) {
        return coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
    }

    /** 将打卡实体转换为 VO，并补齐用户昵称。 */
    private CheckinVO toVO(DailyCheckin checkin) {
        User user = userMapper.selectById(checkin.getUserId());
        CheckinVO vo = new CheckinVO();
        vo.setId(checkin.getId());
        vo.setUserId(checkin.getUserId());
        vo.setNickname(user == null ? "未知用户" : user.getNickname());
        vo.setDate(checkin.getCheckinDate());
        vo.setMoodEmoji(checkin.getMoodEmoji());
        vo.setMoodText(checkin.getMoodText());
        vo.setContent(checkin.getContent());
        return vo;
    }
}
