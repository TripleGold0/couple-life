package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物种类配置实体（对应 pet_type 表）。
 * <p>用于存储平台预置的可选宠物（如猫、狗、兔子、龙、史莱姆），
 * 由运营/开发同学维护，前端在选择页拉取列表展示。</p>
 */
@Data
@TableName("pet_type")
public class PetType {
    /** 主键 */
    private Long id;
    /** 类型唯一编码（CAT/DOG/RABBIT/DRAGON/SLIME...） */
    private String code;
    /** 展示名称 */
    private String name;
    /** 介绍/描述文案 */
    private String description;
    /** 列表图标 / 头像图片 URL */
    private String avatar;
    /** 悬浮挂件主体动画 / 序列帧资源 URL */
    private String spriteUrl;
    /** 排序，数值越小越靠前 */
    private Integer sortOrder;
    /** 是否上架：1 上架，0 下架 */
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
