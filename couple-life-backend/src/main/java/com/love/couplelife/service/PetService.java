package com.love.couplelife.service;

import com.love.couplelife.dto.PetInteractionDTO;
import com.love.couplelife.dto.PetSelectionRequestDTO;
import com.love.couplelife.vo.PetSelectionRequestVO;
import com.love.couplelife.vo.PetTypeVO;
import com.love.couplelife.vo.PetVO;

import java.util.List;

/**
 * 电子宠物业务接口。
 *
 * <p>覆盖 MVP 范围：
 * <ul>
 *     <li>宠物种类列表 / 当前宠物详情</li>
 *     <li>宠物选择请求（双方共同同意机制）</li>
 *     <li>互动行为（喂食 / 抚摸 / 玩耍）</li>
 *     <li>每日属性衰减（由 {@link com.love.couplelife.service.impl.PetScheduledTask} 触发）</li>
 * </ul>
 */
public interface PetService {

    /** 列出全部上架的可选宠物种类。 */
    List<PetTypeVO> listTypes();

    /** 当前情侣的活跃宠物（未绑定情侣或尚未选择宠物时返回 null）。 */
    PetVO currentPet();

    /** 发起宠物选择请求，返回新创建的请求 VO。 */
    PetSelectionRequestVO createSelectionRequest(PetSelectionRequestDTO dto);

    /** 列出当前用户相关（自己发起或需自己确认）的请求。 */
    List<PetSelectionRequestVO> listSelectionRequests();

    /** 同意一个待确认的请求（操作人必须是 partnerId）。 */
    PetVO agreeSelectionRequest(Long requestId);

    /** 拒绝一个待确认的请求（操作人必须是 partnerId）。 */
    void rejectSelectionRequest(Long requestId);

    /** 执行一次互动并返回最新宠物状态。 */
    PetVO interact(PetInteractionDTO dto);

    /**
     * 每日属性衰减处理（饱食度 / 心情值减少；等级与陪伴天数随访问刷新）。
     * <p>由定时任务每日 0 点调用；幂等：以 last_decay_date 防止重复扣减。</p>
     */
    void runDailyDecay();

    /**
     * 将所有过期的 PENDING 选择请求置为 EXPIRED。
     * <p>由定时任务每小时调用；也可在请求列表查询时按需轻量补救。</p>
     */
    void expireOverdueRequests();
}
