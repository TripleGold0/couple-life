package com.love.couplelife.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物选择请求 VO。
 * <p>用于：
 * <ul>
 *     <li>「我」发起的请求 → 等待对方同意</li>
 *     <li>「我」收到的请求 → 待我处理（同意/拒绝）</li>
 * </ul>
 */
@Data
public class PetSelectionRequestVO {
    private Long id;
    private Long coupleId;
    /** 发起方用户 ID */
    private Long requesterId;
    /** 发起方昵称（便于前端展示通知） */
    private String requesterNickname;
    /** 待确认的伴侣用户 ID */
    private Long partnerId;
    /** 宠物种类 ID */
    private Long petTypeId;
    /** 宠物种类名称 */
    private String petTypeName;
    /** 宠物种类编码 */
    private String petTypeCode;
    /** 建议的宠物昵称 */
    private String nickname;
    /** 状态：PENDING / AGREED / REJECTED / EXPIRED */
    private String status;
    /** 过期时间 */
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
