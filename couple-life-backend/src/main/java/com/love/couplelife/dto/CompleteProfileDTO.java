package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 完善个人资料请求体（短信验证码登录后首次进入时强制弹窗使用）。
 * 必填：昵称、性别、密码。
 */
@Data
public class CompleteProfileDTO {
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;
}
