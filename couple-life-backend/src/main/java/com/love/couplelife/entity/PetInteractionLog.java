package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物互动日志（对应 pet_interaction_log 表）。
 *
 * <p>每次喂食 / 抚摸 / 玩耍均落库一条；
 * 用于：1）做每日次数限制（如抚摸每日上限）；2）回放双方互动轨迹；3）日后做统计。</p>
 */
@Data
@TableName("pet_interaction_log")
public class PetInteractionLog {
    private Long id;
    /** 宠物实例 ID */
    private Long petId;
    /** 情侣空间 ID（冗余便于聚合） */
    private Long coupleId;
    /** 互动操作的用户 ID（双方任一） */
    private Long userId;
    /** 互动类型：FEED / PET / PLAY */
    private String action;
    /** 本次互动给宠物增加的亲密度 */
    private Integer intimacyDelta;
    /** 本次互动给宠物增加的饱食度 */
    private Integer fullnessDelta;
    /** 本次互动给宠物增加的心情值 */
    private Integer moodDelta;
    private LocalDateTime createTime;
}
