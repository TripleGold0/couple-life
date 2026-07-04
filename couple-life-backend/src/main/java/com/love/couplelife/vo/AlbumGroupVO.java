package com.love.couplelife.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 相册按日期分组返回 VO。
 * <p>前端按 {@link #date} 倒序展示一段时间内的所有照片。</p>
 */
@Data
public class AlbumGroupVO {
    /** 拍摄日期 */
    private LocalDate date;
    /** 当日所有照片 */
    private List<AlbumImageVO> photos;
}
