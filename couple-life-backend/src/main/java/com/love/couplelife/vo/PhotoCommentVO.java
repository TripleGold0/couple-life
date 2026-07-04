package com.love.couplelife.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册照片评论返回 VO（携带评论者昵称、头像）。
 */
@Data
public class PhotoCommentVO {
    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime createTime;
}
