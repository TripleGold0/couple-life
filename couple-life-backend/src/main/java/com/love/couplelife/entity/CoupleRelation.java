package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣关系实体（对应 couple_relation 表）。
 * <p>
 * 双向存储：A 绑定 B 时会同时插入 (userId=A, partnerId=B) 和 (userId=B, partnerId=A) 两条记录，
 * 共享同一个 coupleId，便于按 coupleId 聚合相册、打卡、旅行等数据。
 * </p>
 */
@Data
@TableName("couple_relation")
public class CoupleRelation {
    /** 主键 */
    private Long id;
    /** 情侣关系 ID（A、B 两条记录共享同一值），业务侧的"情侣空间"键 */
    private Long coupleId;
    /** 当前记录的用户 ID */
    private Long userId;
    /** 伴侣的用户 ID */
    private Long partnerId;
    /** 恋爱开始日期，用于首页"在一起 N 天"展示 */
    private LocalDate loveStartDate;
    /** 关系状态：1 生效，0 已解除 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
