package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物选择请求（对应 pet_selection_request 表）。
 *
 * <p>选择 / 更换宠物需要情侣双方共同同意：
 * <ol>
 *     <li>{@link #requesterId} 一方发起请求，状态 PENDING；</li>
 *     <li>系统通知 {@link #partnerId} 对方；</li>
 *     <li>对方同意 → 状态 AGREED → 服务层创建/替换 {@link Pet}；</li>
 *     <li>对方拒绝 → REJECTED；超过 {@link #expireTime} 自动 EXPIRED。</li>
 * </ol>
 */
@Data
@TableName("pet_selection_request")
public class PetSelectionRequest {
    private Long id;
    /** 情侣空间 ID */
    private Long coupleId;
    /** 发起方用户 ID */
    private Long requesterId;
    /** 需要确认的伴侣用户 ID */
    private Long partnerId;
    /** 希望选择 / 更换的宠物种类 ID */
    private Long petTypeId;
    /** 本次请求建议的宠物昵称（可选） */
    private String nickname;
    /** 状态：PENDING / AGREED / REJECTED / EXPIRED */
    private String status;
    /** 请求过期时间（默认 24 小时） */
    private LocalDateTime expireTime;
    /** 伴侣同意 / 拒绝的时间 */
    private LocalDateTime decidedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
