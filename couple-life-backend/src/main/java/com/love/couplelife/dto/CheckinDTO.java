package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日心情打卡请求体（新增/更新）。
 * <p>同一用户同一日期只能存在一条打卡记录，后端使用 upsert 语义。</p>
 */
@Data
public class CheckinDTO {
    /** 打卡日期 */
    @NotNull(message = "打卡日期不能为空")
    private LocalDate checkinDate;

    /** 心情 emoji */
    @NotBlank(message = "请选择心情表情")
    private String moodEmoji;

    /** 心情文字标签（可空） */
    private String moodText;

    /** 打卡正文（可空） */
    private String content;
}
