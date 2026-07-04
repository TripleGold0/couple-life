package com.love.couplelife.service;

import com.love.couplelife.dto.CounselorChatDTO;
import com.love.couplelife.vo.CounselorReplyVO;

/**
 * 情感咨询师业务接口（基于 LLM）。
 */
public interface CounselorService {

    /**
     * 调用 LLM，基于情侣情感调节顾问 system prompt 生成回复。
     */
    CounselorReplyVO chat(CounselorChatDTO dto);
}
