package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 旅行记录的图片实体（对应 travel_image 表）。
 * <p>一条旅行记录可以挂多张图片，按 sortOrder 升序展示。</p>
 */
@Data
@TableName("travel_image")
public class TravelImage {
    private Long id;
    /** 所属旅行记录 ID */
    private Long travelId;
    /** 图片 URL */
    private String imageUrl;
    /** 排序值，越小越靠前 */
    private Integer sortOrder;
    private LocalDateTime createTime;
}
