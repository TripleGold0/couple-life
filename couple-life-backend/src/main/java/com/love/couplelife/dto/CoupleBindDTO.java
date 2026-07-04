package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 绑定情侣请求体。
 * <p>用户输入对方的邀请码以建立双向情侣关系；恋爱开始日期可选，未填则取当日。</p>
 */
@Data
public class CoupleBindDTO {
    /** 对方的邀请码 */
    @NotBlank(message = "请输入对方的邀请码")
    private String inviteCode;

    /** 恋爱开始日期（可选） */
    private LocalDate loveStartDate;
}
