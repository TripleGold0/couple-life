package com.love.couplelife.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页聚合 VO。
 * <p>一次性返回首页所需的"在一起天数"、最近打卡、最近旅行、最近相册图片，避免前端发起多次请求。</p>
 */
@Data
public class HomeSummaryVO {
    /** 在一起天数 */
    private Long loveDays;
    /** 最近打卡列表 */
    private List<CheckinVO> recentCheckins;
    /** 最近旅行记录 */
    private List<TravelRecordVO> recentTravels;
    /** 最近上传的照片 */
    private List<AlbumImageVO> recentPhotos;
}
