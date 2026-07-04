package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录验证码实体（对应 login_captcha 表）。
 * <p>用于短信/邮箱验证码登录、注册、绑定等场景。一条验证码只能消费一次。</p>
 */
@Data
@TableName("login_captcha")
public class LoginCaptcha {
    private Long id;
    /** 接收账号：手机号或邮箱 */
    private String account;
    /** 验证码（明文，便于演示；生产建议存哈希） */
    private String captchaCode;
    /** 验证码类型：sms / email / login / register / bind */
    private String captchaType;
    /** 过期时间，过期后无法再校验 */
    private LocalDateTime expireTime;
    /** 是否已使用：1 已用，0 未用 */
    private Integer used;
    private LocalDateTime createTime;
}
