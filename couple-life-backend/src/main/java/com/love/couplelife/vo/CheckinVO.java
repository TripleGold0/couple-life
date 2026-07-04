package com.love.couplelife.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 心情打卡返回 VO。
 * <p>用于"我的打卡日历"以及首页双方打卡聚合展示。</p>
 */
@Data
public class CheckinVO {
    private Long id;
    /** 打卡用户 ID */
    private Long userId;
    /** 打卡用户昵称（关联查询） */
    private String nickname;
    private LocalDate date;
    private String moodEmoji;
    private String moodText;
    private String content;
}
