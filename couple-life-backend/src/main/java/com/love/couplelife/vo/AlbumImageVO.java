package com.love.couplelife.vo;

import lombok.Data;

/**
 * 相册图片返回 VO（精简版，列表页使用）。
 */
@Data
public class AlbumImageVO {
    private Long id;
    private String imageUrl;
    private String title;
    private String description;
}
