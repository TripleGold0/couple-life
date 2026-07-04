package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日心情打卡实体（对应 daily_checkin 表）。
 * <p>同一用户同一天仅允许一条记录（业务层约束）。</p>
 */
@Data
@TableName("daily_checkin")
public class DailyCheckin {
    private Long id;
    /** 打卡用户 ID */
    private Long userId;
    /** 所属情侣空间 ID（未绑定情侣时可能为 null） */
    private Long coupleId;
    /** 打卡日期 */
    private LocalDate checkinDate;
    /** 心情 emoji，如 😊 */
    private String moodEmoji;
    /** 心情文字标签，如 "开心" */
    private String moodText;
    /** 打卡正文 */
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
