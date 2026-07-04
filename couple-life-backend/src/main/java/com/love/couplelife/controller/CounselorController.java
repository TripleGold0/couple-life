package com.love.couplelife.controller;

import com.love.couplelife.common.Result;
import com.love.couplelife.dto.CounselorChatDTO;
import com.love.couplelife.service.CounselorService;
import com.love.couplelife.vo.CounselorReplyVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 情感咨询师控制器。
 * <p>
 * 业务模块：基于大模型的情感 / 情侣关系咨询对话入口，
 * 接收用户问题并返回 AI 给出的咨询回复。
 * </p>
 * <p>
 * 统一前缀路径：{@code /api/counselor}<br>
 * 鉴权要求：所有接口均需登录（JWT 鉴权）。
 * </p>
 */
@RestController
@RequestMapping("/api/counselor")
public class CounselorController {

    private final CounselorService counselorService;

    public CounselorController(CounselorService counselorService) {
        this.counselorService = counselorService;
    }

    /**
     * 与 AI 情感咨询师进行一次对话。
     * <p>HTTP: {@code POST /api/counselor/chat}</p>
     *
     * @param dto 对话参数（用户提问内容、上下文消息等）
     * @return AI 回复内容 {@link CounselorReplyVO}
     */
    @PostMapping("/chat")
    public Result<CounselorReplyVO> chat(@RequestBody CounselorChatDTO dto) {
        return Result.success(counselorService.chat(dto));
    }
}
