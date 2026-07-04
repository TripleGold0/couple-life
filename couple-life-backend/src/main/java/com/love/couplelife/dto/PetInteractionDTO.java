package com.love.couplelife.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 宠物互动 DTO。
 * <p>action 取值：
 * <ul>
 *     <li>FEED 喂食：消耗"食物"提升饱食度与亲密度（MVP 阶段不引入背包，直接允许）</li>
 *     <li>PET 抚摸：每日有限次（默认 5 次），提升心情与亲密度</li>
 *     <li>PLAY 玩耍：扩展互动，消耗心情上限较低，亲密度收益中等</li>
 * </ul>
 */
@Data
public class PetInteractionDTO {
    /** 互动类型：FEED / PET / PLAY */
    @NotBlank(message = "互动类型不能为空")
    private String action;
}
