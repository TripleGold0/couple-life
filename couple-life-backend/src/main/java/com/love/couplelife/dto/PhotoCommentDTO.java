package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 相册照片评论请求体。
 */
@Data
public class PhotoCommentDTO {
    /** 评论正文 */
    @NotBlank(message = "评论内容不能为空")
    private String content;
}
