package com.love.couplelife.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 个人资料更新请求体（"我的"页面）。
 * <p>所有字段均为可选，仅更新非 null 字段。</p>
 */
@Data
public class UserProfileDTO {
    private String nickname;
    private Integer gender;
    private String avatar;
    private LocalDate birthday;
}
