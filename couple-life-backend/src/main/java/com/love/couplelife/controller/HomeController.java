package com.love.couplelife.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.love.couplelife.common.Result;
import com.love.couplelife.entity.AlbumImage;
import com.love.couplelife.entity.CoupleRelation;
import com.love.couplelife.entity.DailyCheckin;
import com.love.couplelife.entity.User;
import com.love.couplelife.mapper.AlbumImageMapper;
import com.love.couplelife.mapper.CoupleRelationMapper;
import com.love.couplelife.mapper.DailyCheckinMapper;
import com.love.couplelife.mapper.UserMapper;
import com.love.couplelife.service.TravelService;
import com.love.couplelife.util.SecurityUtil;
import com.love.couplelife.vo.AlbumImageVO;
import com.love.couplelife.vo.CheckinVO;
import com.love.couplelife.vo.HomeSummaryVO;
import com.love.couplelife.vo.TravelRecordVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页聚合控制器。
 * <p>
 * 业务模块：为前端首页提供一次性聚合数据，包括恋爱天数、最近的打卡记录、
 * 最近的旅行记录、最近的相册照片等，避免前端多次独立请求。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/home}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。当前用户未绑定情侣时，仅返回个人维度可用的数据，
 * 旅行 / 相册等共享数据返回空列表。
 * </p>
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final CoupleRelationMapper coupleRelationMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final AlbumImageMapper albumImageMapper;
    private final UserMapper userMapper;
    private final TravelService travelService;

    public HomeController(CoupleRelationMapper coupleRelationMapper, DailyCheckinMapper dailyCheckinMapper, AlbumImageMapper albumImageMapper, UserMapper userMapper, TravelService travelService) {
        this.coupleRelationMapper = coupleRelationMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.albumImageMapper = albumImageMapper;
        this.userMapper = userMapper;
        this.travelService = travelService;
    }

    /**
     * 获取首页聚合摘要：恋爱天数、最近打卡、最近旅行、最近相册。
     * <p>HTTP: {@code GET /api/home/summary}</p>
     *
     * @return 首页聚合数据 {@link HomeSummaryVO}
     */
    @GetMapping("/summary")
    public Result<HomeSummaryVO> summary() {
        // 从 Spring Security 上下文中取得当前登录用户 ID
        Long userId = SecurityUtil.currentUserId();
        HomeSummaryVO vo = new HomeSummaryVO();
        // 查询当前用户处于"已绑定（status=1）"状态的情侣关系，limit 1 仅取一条
        CoupleRelation relation = coupleRelationMapper.selectOne(new LambdaQueryWrapper<CoupleRelation>()
                .eq(CoupleRelation::getUserId, userId)
                .eq(CoupleRelation::getStatus, 1)
                .last("limit 1"));
        if (relation != null && relation.getLoveStartDate() != null) {
            // 恋爱天数 = 当前日期 - 恋爱开始日期 + 1（包含开始当天）
            vo.setLoveDays(ChronoUnit.DAYS.between(relation.getLoveStartDate(), LocalDate.now()) + 1);
        }

        // 收集需要查询打卡的用户 ID 集合：自己 + 伴侣（如已绑定）
        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);
        if (relation != null) {
            userIds.add(relation.getPartnerId());
        }
        // 取双方最近 6 条打卡，按打卡日期倒序
        List<DailyCheckin> checkins = dailyCheckinMapper.selectList(new LambdaQueryWrapper<DailyCheckin>()
                .in(DailyCheckin::getUserId, userIds)
                .orderByDesc(DailyCheckin::getCheckinDate)
                .last("limit 6"));
        // 将实体转换为前端需要的 VO，并填充昵称（按 userId 查 User）
        vo.setRecentCheckins(checkins.stream().map(item -> {
            User user = userMapper.selectById(item.getUserId());
            CheckinVO c = new CheckinVO();
            c.setId(item.getId());
            c.setUserId(item.getUserId());
            c.setNickname(user == null ? "" : user.getNickname());
            c.setDate(item.getCheckinDate());
            c.setMoodEmoji(item.getMoodEmoji());
            c.setMoodText(item.getMoodText());
            c.setContent(item.getContent());
            return c;
        }).toList());

        if (relation != null) {
            // 已绑定情侣：取最近 3 条旅行记录
            List<TravelRecordVO> travels = travelService.list();
            vo.setRecentTravels(travels.stream().limit(3).toList());
            // 取该情侣空间下未删除的最近 8 张相册照片，按 ID 倒序
            List<AlbumImage> photos = albumImageMapper.selectList(new LambdaQueryWrapper<AlbumImage>()
                    .eq(AlbumImage::getCoupleId, relation.getCoupleId())
                    .eq(AlbumImage::getDeleted, 0)
                    .orderByDesc(AlbumImage::getId)
                    .last("limit 8"));
            vo.setRecentPhotos(photos.stream().map(image -> {
                AlbumImageVO album = new AlbumImageVO();
                album.setId(image.getId());
                album.setImageUrl(image.getImageUrl());
                album.setTitle(image.getTitle());
                album.setDescription(image.getDescription());
                return album;
            }).toList());
        } else {
            // 未绑定情侣：旅行 / 相册数据置为空，避免前端空指针
            vo.setRecentTravels(List.of());
            vo.setRecentPhotos(List.of());
        }
        return Result.success(vo);
    }
}
