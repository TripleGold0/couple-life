package com.love.couplelife.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求体。
 * 业务约束：phone 和 email 二选一（至少且仅校验任一格式合法）；具体的“至少有一项”校验在 service 层完成。
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotNull(message = "性别不能为空")
    private Integer gender;

    /** 手机号（与 email 二选一）。若填写，需符合手机号格式 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱（与 phone 二选一）。若填写，需符合邮箱格式 */
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @NotBlank(message = "验证码不能为空")
    private String captcha;
}
