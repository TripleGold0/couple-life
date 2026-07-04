package com.love.couplelife.dto;

import lombok.Data;

import java.util.List;

/**
 * 情感咨询师对话请求体。
 * <p>
 * 后端将 messages 拼接系统提示词后转发给 LLM（OpenAI 兼容协议）。
 * baseUrl / apiKey / model 三者均可由用户在前端覆盖默认配置，便于接入私有大模型。
 * </p>
 */
@Data
public class CounselorChatDTO {

    /**
     * 历史对话消息（不含 system），按时间顺序，最后一条应为本次用户输入。
     */
    private List<Message> messages;

    /** 可选：用户自定义 LLM baseUrl（OpenAI 兼容协议，例如 https://api.openai.com） */
    private String baseUrl;

    /** 可选：用户自定义 API Key */
    private String apiKey;

    /** 可选：用户自定义模型名 */
    private String model;

    /**
     * 单条对话消息。
     */
    @Data
    public static class Message {
        /** 角色：user（用户）/ assistant（AI 回复） */
        private String role;
        /** 消息正文 */
        private String content;
    }
}
