package com.love.couplelife.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 当前情侣宠物详情 VO（{@code GET /api/pet/current} 返回）。
 *
 * <p>未绑定情侣或情侣尚未选择宠物时，由前端处理 null 场景（控制器仅在已有宠物时返回非空）。</p>
 */
@Data
public class PetVO {
    /** 宠物实例 ID */
    private Long id;
    /** 所属情侣空间 ID */
    private Long coupleId;
    /** 宠物种类 ID */
    private Long petTypeId;
    /** 宠物种类编码（CAT/DOG/...，前端可据此选择动画资源） */
    private String petTypeCode;
    /** 宠物种类展示名称 */
    private String petTypeName;
    /** 宠物种类立绘 / 主体动画资源 */
    private String spriteUrl;
    /** 宠物种类头像（小图标） */
    private String typeAvatar;
    /** 昵称 */
    private String nickname;
    /** 亲密度 */
    private Integer intimacy;
    /** 饱食度 0-100 */
    private Integer fullness;
    /** 心情值 0-100 */
    private Integer mood;
    /** 等级 */
    private Integer level;
    /** 成长阶段 */
    private String stage;
    /** 绑定日期 */
    private LocalDate boundDate;
    /** 陪伴天数（自绑定日起累计，含今日） */
    private Long companionDays;
}
