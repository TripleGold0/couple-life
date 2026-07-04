package com.love.couplelife.service.impl;

import com.love.couplelife.service.PetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 电子宠物定时任务。
 *
 * <p>职责：
 * <ul>
 *     <li>每日 00:05 调用 {@link PetService#runDailyDecay()}：扣减饱食度 / 心情值，并刷新衰减日期。</li>
 *     <li>每小时整点调用 {@link PetService#expireOverdueRequests()}：将超时未处理的选择请求置为 EXPIRED。</li>
 * </ul>
 *
 * <p>设计依据：参考 {@code CheckinServiceImpl} 的"由 Service 承载业务、由独立组件触发"的风格，
 * 任务本身只做调用与基础异常隔离，所有业务逻辑仍在 Service 层，便于单测。</p>
 */
@Component
public class PetScheduledTask {

    private final PetService petService;

    public PetScheduledTask(PetService petService) {
        this.petService = petService;
    }

    /** 每日 00:05 执行一次属性衰减。错峰避开 0 点其他业务。 */
    @Scheduled(cron = "0 5 0 * * *")
    public void dailyDecay() {
        petService.runDailyDecay();
    }

    /** 每小时整点把过期的选择请求置为 EXPIRED。 */
    @Scheduled(cron = "0 0 * * * *")
    public void expireRequests() {
        petService.expireOverdueRequests();
    }
}
