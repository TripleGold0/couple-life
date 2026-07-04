package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行记录新增/更新请求体。
 */
@Data
public class TravelRecordDTO {
    /** 地点名称（必填，列表展示用） */
    @NotBlank(message = "地点名称不能为空")
    private String locationName;
    /** 国家 */
    private String country;
    /** 城市 */
    private String city;

    /** 经度（地图打点用） */
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;

    /** 纬度（地图打点用） */
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    /** 旅行日期 */
    @NotNull(message = "旅游日期不能为空")
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
