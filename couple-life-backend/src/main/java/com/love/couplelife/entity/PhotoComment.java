package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 相册照片评论实体（对应 photo_comment 表）。
 */
@Data
@TableName("photo_comment")
public class PhotoComment {
    private Long id;
    /** 关联的相册图片 ID */
    private Long photoId;
    /** 评论用户 ID */
    private Long userId;
    /** 评论正文 */
    private String content;
    private LocalDateTime createTime;
    /** 软删除：0 正常，1 已删除 */
    private Integer deleted;
}
