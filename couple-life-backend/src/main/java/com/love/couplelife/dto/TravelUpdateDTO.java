package com.love.couplelife.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行记录更新请求体（所有字段可选，仅更新非 null 字段）。
 */
@Data
public class TravelUpdateDTO {
    /** 地点名称 */
    private String locationName;
    /** 国家 */
    private String country;
    /** 城市 */
    private String city;
    /** 经度（地图打点用） */
    private BigDecimal longitude;
    /** 纬度（地图打点用） */
    private BigDecimal latitude;
    /** 旅行日期 */
    private LocalDate travelDate;
    /** 简要概述 */
    private String summary;
    /** 详细描述 */
    private String detail;
    /** 我的感受 */
    private String myFeeling;
    /** 伴侣的感受 */
    private String partnerFeeling;
    /** 关联图片 URL 列表，按顺序展示 */
    private List<String> imageUrls;
}
