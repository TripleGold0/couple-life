package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体。支持两种登录方式，由 {@link #loginType} 切换：
 * <ul>
 *     <li>PASSWORD：账号密码 + 图形验证码</li>
 *     <li>SMS_CODE：手机号 + 短信验证码</li>
 * </ul>
 */
@Data
public class LoginDTO {
    /**
     * 登录方式：PASSWORD（账号密码 + 图形验证码） / SMS_CODE（手机号 + 短信验证码）
     */
    @NotBlank(message = "登录方式不能为空")
    private String loginType;

    /** 登录账号：用户名 / 手机号 / 邮箱 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 仅 PASSWORD 模式必填 */
    private String password;

    /** PASSWORD 模式：图形验证码；SMS_CODE 模式：短信验证码 */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /** PASSWORD 模式必填，对应图形验证码的 key */
    private String captchaKey;
}
