package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣宠物实例实体（对应 pet 表）。
 *
 * <p>每对情侣同一时刻仅有一只 status=1 的活跃宠物，
 * 该约束由 Service 层（{@code PetServiceImpl#agreeSelectionRequest}）通过
 * 「先把旧宠物 status 置 0、再插入新宠物」保证（MySQL 不支持部分唯一索引）。</p>
 *
 * <p>关键属性：
 * <ul>
 *     <li>{@link #intimacy} 亲密度：双方互动累计共享，决定宠物等级 / 阶段</li>
 *     <li>{@link #fullness} 饱食度、{@link #mood} 心情值：每日衰减，需互动维持</li>
 *     <li>{@link #boundDate} 绑定日期：用于推导陪伴天数（{@code today - boundDate + 1}）</li>
 *     <li>{@link #lastDecayDate} 最近一次衰减日期：保证定时任务幂等，不会同一天重复扣减</li>
 * </ul>
 */
@Data
@TableName("pet")
public class Pet {
    /** 主键 */
    private Long id;
    /** 所属情侣空间 ID（来自 couple_relation.couple_id） */
    private Long coupleId;
    /** 宠物种类 ID（外键 pet_type.id） */
    private Long petTypeId;
    /** 宠物昵称（可空） */
    private String nickname;
    /** 亲密度（情侣共享，互动累计） */
    private Integer intimacy;
    /** 饱食度 0-100，每日衰减 */
    private Integer fullness;
    /** 心情值 0-100，每日衰减 */
    private Integer mood;
    /** 等级（依据亲密度自动成长） */
    private Integer level;
    /** 成长阶段：BABY / TEEN / ADULT */
    private String stage;
    /** 宠物绑定（生效）日期，用于计算陪伴天数 */
    private LocalDate boundDate;
    /** 最近一次属性衰减处理的日期 */
    private LocalDate lastDecayDate;
    /** 状态：1 活跃，0 已弃养/被替换 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
