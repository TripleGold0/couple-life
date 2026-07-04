package com.love.couplelife.vo;

import lombok.Data;

/**
 * 用户基础信息返回 VO。
 * <p>不包含密码、删除标记等敏感字段；如已绑定情侣，{@link #partner} 内嵌伴侣的同结构信息（partner 字段不再递归）。</p>
 */
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String nickname;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    /** 邀请码：用于让伴侣绑定 */
    private String inviteCode;
    /** 资料是否完善：1 已完善，0 待完善 */
    private Integer profileCompleted;
    /** 恋爱开始日期（来自 couple_relation） */
    private java.time.LocalDate loveStartDate;
    /** 伴侣信息；未绑定时为 null */
    private UserInfoVO partner;
    /** 个人侧悬浮宠物开关：1 显示，0 关闭（默认为 1） */
    private Integer petDisplayEnabled;
}
