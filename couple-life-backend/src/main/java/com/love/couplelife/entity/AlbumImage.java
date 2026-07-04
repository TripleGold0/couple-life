package com.love.couplelife.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 相册图片实体（对应 album_image 表）。
 * <p>每张图片归属于某对情侣，由其中一方上传；按 shootDate（拍摄日期）分组展示。</p>
 */
@Data
@TableName("album_image")
public class AlbumImage {
    private Long id;
    /** 所属情侣空间 ID */
    private Long coupleId;
    /** 上传者用户 ID */
    private Long uploaderId;
    /** 图片访问 URL（相对路径，由 FileUploadUtil 生成） */
    private String imageUrl;
    /** 拍摄日期，前端按此字段做时间轴分组 */
    private LocalDate shootDate;
    /** 图片标题（可空） */
    private String title;
    /** 图片描述（可空） */
    private String description;
    private LocalDateTime createTime;
    /** 软删除：0 正常，1 已删除 */
    private Integer deleted;
}
