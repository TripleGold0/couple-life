package com.love.couplelife.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 旅行记录返回 VO。
 * <p>{@link #coverImage} 为列表展示用的封面，{@link #images} 为详情页展示用的全部图片。</p>
 */
@Data
public class TravelRecordVO {
    private Long id;
    private String locationName;
    private String country;
    private String city;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private LocalDate travelDate;
    private String summary;
    private String detail;
    private String myFeeling;
    private String partnerFeeling;
    /** 封面图（取第一张） */
    private String coverImage;
    /** 详情页全部图片 */
    private List<String> images;
}
