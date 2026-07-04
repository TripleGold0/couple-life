package com.love.couplelife.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起宠物选择请求的 DTO。
 * <p>由情侣的任一方发起，需要对方同意后才生效。</p>
 */
@Data
public class PetSelectionRequestDTO {
    /** 期望选择 / 更换的宠物种类 ID */
    @NotNull(message = "请选择宠物种类")
    private Long petTypeId;
    /** 可选：本次请求建议的宠物昵称 */
    private String nickname;
}
