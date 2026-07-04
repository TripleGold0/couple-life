package com.love.couplelife.vo;

import lombok.Data;

/** 宠物种类 VO（选择列表使用）。 */
@Data
public class PetTypeVO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String avatar;
    private String spriteUrl;
    private Integer sortOrder;
}
