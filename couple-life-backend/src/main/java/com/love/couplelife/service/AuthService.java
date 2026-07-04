package com.love.couplelife.service;

import com.love.couplelife.dto.CaptchaDTO;
import com.love.couplelife.dto.LoginDTO;
import com.love.couplelife.dto.RegisterDTO;
import com.love.couplelife.vo.LoginVO;

import java.util.Map;

/**
 * 登录认证业务接口。
 * <p>支持两种登录方式：账号密码 + 图形验证码、手机号 + 短信验证码。</p>
 */
public interface AuthService {

    /**
     * 下发短信/邮箱验证码。
     *
     * @return 提示信息（演示场景下会同时返回明文验证码，便于联调）
     */
    Map<String, String> sendCaptcha(CaptchaDTO dto);

    /**
     * 生成图形验证码。
     *
     * @return key（用于校验时回传）+ base64 图片数据
     */
    Map<String, String> generateImageCaptcha();

    /**
     * 注册。校验完成后插入用户记录并生成默认邀请码。
     *
     * @return 包含新用户 id 的 Map
     */
    Map<String, Long> register(RegisterDTO dto);

    /**
     * 登录，成功返回 JWT token + 用户信息。
     */
    LoginVO login(LoginDTO dto);
}
