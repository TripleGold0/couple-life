package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅行记录实体（对应 travel_record 表）。
 * <p>用于记录情侣共同旅行的目的地、感受、图片等，可在地图上打点展示。</p>
 */
@Data
@TableName("travel_record")
public class TravelRecord {
    private Long id;
    /** 所属情侣空间 ID */
    private Long coupleId;
    /** 创建者用户 ID */
    private Long creatorId;
    /** 地点名称（用户填写或地图选点） */
    private String locationName;
    /** 国家 */
    private String country;
    /** 城市 */
    private String city;
    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;
    /** 旅行日期 */
    private LocalDate travelDate;
    /** 简要概述（列表页展示） */
    private String summary;
    /** 详细描述（详情页展示） */
    private String detail;
    /** 我的感受 */
    private String myFeeling;
    /** 伴侣的感受 */
    private String partnerFeeling;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 软删除：0 正常，1 已删除 */
    private Integer deleted;
}
