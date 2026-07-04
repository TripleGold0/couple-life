package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证码下发请求体。
 * <p>用于"获取短信/邮箱验证码"接口，由 {@link com.love.couplelife.controller.AuthController} 接收。</p>
 */
@Data
public class CaptchaDTO {
    /** 短信验证码：手机号；邮箱注册：邮箱 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 验证码用途：LOGIN（短信登录）/ REGISTER（注册） */
    @NotBlank(message = "验证码类型不能为空")
    private String type;
}
