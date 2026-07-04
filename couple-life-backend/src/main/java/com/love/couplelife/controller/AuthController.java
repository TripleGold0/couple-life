package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.CaptchaDTO;
import com.love.couplelife.dto.LoginDTO;
import com.love.couplelife.dto.RegisterDTO;
import com.love.couplelife.service.AuthService;
import com.love.couplelife.vo.LoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 鉴权控制器。
 * <p>
 * 业务模块：用户认证体系入口，包括短信 / 邮箱验证码下发、图形验证码生成、
 * 用户注册以及登录（账号密码登录、短信验证码登录）。登录成功后由服务层签发 JWT。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/auth}<br>
 * 鉴权要求：本控制器下所有接口均为匿名可访问，无需登录态（位于 Spring Security 放行白名单中）。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 发送短信/邮箱验证码（短信登录、注册等）。
     * <p>HTTP: {@code POST /api/auth/captcha}</p>
     *
     * @param dto 验证码请求参数（手机号 / 邮箱、用途场景等）
     * @return 包含发送结果信息的 Map（如验证码 key、过期时间等）
     */
    @PostMapping("/captcha")
    public Result<Map<String, String>> captcha(@Valid @RequestBody CaptchaDTO dto) {
        return Result.success("验证码已发送", authService.sendCaptcha(dto));
    }

    /**
     * 获取图形验证码（账号密码登录使用）。
     * <p>HTTP: {@code GET /api/auth/image-captcha}</p>
     *
     * @return 图形验证码信息，包含验证码 key 与 base64 图片内容
     */
    @GetMapping("/image-captcha")
    public Result<Map<String, String>> imageCaptcha() {
        return Result.success(authService.generateImageCaptcha());
    }

    /**
     * 用户注册。
     * <p>HTTP: {@code POST /api/auth/register}</p>
     *
     * @param dto 注册参数（手机号、密码、验证码等）
     * @return 注册成功后返回的用户 ID，键名为 {@code id}
     */
    @PostMapping("/register")
    public Result<Map<String, Long>> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    /**
     * 用户登录，支持账号密码登录与短信验证码登录两种模式。
     * <p>HTTP: {@code POST /api/auth/login}</p>
     *
     * @param dto 登录参数（登录方式、账号、密码 / 验证码、图形验证码等）
     * @return 登录成功后的信息 {@link LoginVO}，包含 JWT token、用户基础信息等
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }
}
