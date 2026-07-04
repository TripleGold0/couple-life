package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体（对应 sys_user 表）。
 * <p>同时承载账号信息与个人资料；密码字段以 BCrypt 形式存储。</p>
 * <p>软删除字段：deleted = 1 表示已删除，查询时需过滤。</p>
 */
@Data
@TableName("sys_user")
public class User {
    /** 主键 */
    private Long id;
    /** 登录账号（唯一） */
    private String username;
    /** 昵称，对外展示用 */
    private String nickname;
    /** 性别：0 未知，1 男，2 女 */
    private Integer gender;
    /** 手机号，可用于短信验证码登录 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** BCrypt 加密后的密码 */
    private String password;
    /** 头像 URL */
    private String avatar;
    /** 生日 */
    private LocalDate birthday;
    /** 邀请码：由本用户生成，用于邀请伴侣绑定情侣关系 */
    private String inviteCode;
    /** 账号状态：1 正常，0 禁用 */
    private Integer status;
    /** 资料是否完善：1 已完善，0 待完善（短信验证码首次登录注册时为 0） */
    private Integer profileCompleted;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
    /** 软删除标记：0 正常，1 已删除 */
    private Integer deleted;
    /** 个人侧悬浮宠物开关：1 显示，0 关闭。仅影响自身端展示，不影响数据累计 */
    private Integer petDisplayEnabled;
}
