package com.love.couplelife.vo;

import lombok.Data;

/**
 * 登录成功返回 VO。
 * <p>包含 JWT token 和用户基础信息，前端拿到后存入 Pinia store 并写入 localStorage。</p>
 */
@Data
public class LoginVO {
    /** JWT token，前端需在后续请求头 Authorization: Bearer xxx 中携带 */
    private String token;
    /** 当前登录用户信息 */
    private UserInfoVO user;
}
