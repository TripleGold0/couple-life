package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.CompleteProfileDTO;
import com.love.couplelife.dto.UserProfileDTO;
import com.love.couplelife.service.UserService;
import com.love.couplelife.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器。
 * <p>
 * 业务模块：当前登录用户的个人信息查询与维护，包括获取自身信息、
 * 修改个人资料以及短信登录后的首次资料完善。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/user}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。
 * </p>
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户的基础信息。
     * <p>HTTP: {@code GET /api/user/me}</p>
     *
     * @return 当前用户信息 {@link UserInfoVO}
     */
    @GetMapping("/me")
    public Result<UserInfoVO> currentUser() {
        return Result.success(userService.currentUser());
    }

    /**
     * 修改当前用户个人资料（头像、昵称、性别、生日等可选字段）。
     * <p>HTTP: {@code PUT /api/user/profile}</p>
     *
     * @param dto 待修改的资料字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success("更新成功", null);
    }

    /**
     * 短信登录首次登录后强制完善资料（昵称、性别、密码）。
     * <p>HTTP: {@code POST /api/user/complete-profile}</p>
     *
     * @param dto 必填的完善资料字段（昵称、性别、密码等）
     * @return 无业务数据的成功结果
     */
    @PostMapping("/complete-profile")
    public Result<Void> completeProfile(@Valid @RequestBody CompleteProfileDTO dto) {
        userService.completeProfile(dto);
        return Result.success("资料已完善", null);
    }

    /**
     * 切换悬浮宠物在「我侧」是否显示。
     * <p>HTTP: {@code PUT /api/user/pet-display}</p>
     * <p>请求体：{@code {"enabled": true|false}}</p>
     * <p>注意：此开关仅作用于当前用户端的渲染，不影响另一半的展示与宠物数据本身。</p>
     *
     * @param body JSON 对象，仅包含 enabled 字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/pet-display")
    public Result<Void> updatePetDisplay(@RequestBody java.util.Map<String, Boolean> body) {
        boolean enabled = body != null && Boolean.TRUE.equals(body.get("enabled"));
        userService.updatePetDisplay(enabled);
        return Result.success("已更新", null);
    }
}
