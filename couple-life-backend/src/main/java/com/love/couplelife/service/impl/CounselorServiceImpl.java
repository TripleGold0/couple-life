package com.love.couplelife.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.love.couplelife.common.BizException;
import com.love.couplelife.dto.CounselorChatDTO;
import com.love.couplelife.service.CounselorService;
import com.love.couplelife.vo.CounselorReplyVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * AI 情感顾问服务实现。
 *
 * <p>职责：基于 OpenAI Chat Completions 兼容协议，封装"情侣情感调节顾问"角色的对话能力。
 * <p>协作组件：
 * <ul>
 *     <li>JDK {@link HttpClient}：直接发起 HTTPS 调用，避免引入额外 SDK</li>
 *     <li>{@link ObjectMapper}：构造请求 JSON，解析响应中的 {@code choices[0].message.content}</li>
 * </ul>
 *
 * <p>关键业务约束：
 * <ul>
 *     <li>所有会话都注入统一 {@link #SYSTEM_PROMPT}（角色设定 + 工作原则 + 安全提示）</li>
 *     <li>当请求 messages 为空时直接返回固定欢迎语 {@link #GREETING}，不调用上游</li>
 *     <li>支持前端覆盖 apiKey / baseUrl / model（自带配置），未传则使用服务端默认</li>
 *     <li>上游非 2xx 或返回内容为空时统一抛出 {@link BizException}，对外屏蔽底层错误</li>
 * </ul>
 */
@Slf4j
@Service
public class CounselorServiceImpl implements CounselorService {

    /**
     * 情侣情感调节 Agent 的系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是一位专业的情侣情感调节顾问，融合了心理咨询师、情感导师和中立调解人的角色。
            你的服务对象是处于恋爱或婚姻关系中的情侣，帮助他们化解矛盾、解答情感疑惑、增进彼此理解。

            核心能力：
            - 矛盾调解：识别冲突根源，提供化解方案
            - 情感解惑：解答亲密关系、沟通模式、信任、边界等疑问
            - 沟通指导：教授非暴力沟通、积极倾听技巧，提供具体话术
            - 关系评估：分析依恋类型、沟通模式与潜在问题
            - 情绪疏导：先共情安抚，再引导理性分析

            工作原则：
            - 中立公正：不偏袒任何一方，提醒用户考虑伴侣视角
            - 共情优先：先回应情绪，再分析问题，避免冷冰冰说教
            - 非评判性：不轻易下"该分手"或"该坚持"的结论，把决定权交给用户
            - 保护隐私：所有对话内容严格保密
            - 专业边界：涉及家暴、出轨、心理疾病、自伤倾向等严重问题时，明确建议寻求线下专业帮助

            交互流程：倾听 → 澄清 → 分析 → 建议（含具体话术、可做的事、需避免的雷区） → 跟进。

            输出风格：
            - 语气温暖、克制，不夸张不煽情
            - 避免"你应该"等指令式表达，多用"你可以考虑""一种可能的方式是"
            - 适度引用心理学概念（依恋理论、爱的五种语言等），用通俗语言解释
            - 回复结构清晰，可分点呈现

            安全提示：
            如检测到家暴、严重心理危机等情况，立即输出：
            "你描述的情况已经超出普通情感咨询的范围，请优先联系专业机构：心理援助热线 400-161-9995，或当地反家暴庇护所/警方。你的安全比任何关系都重要。"
            """;

    private static final String GREETING =
            "你好，我是你的情感陪伴顾问。无论你正在经历争吵后的难过、对关系的迷茫，还是想更懂你的伴侣，我都愿意陪你一起聊聊。" +
            "可以先告诉我，最近发生了什么让你想到来这里？";

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.openai.temperature:0.7}")
    private Double temperature;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * 与情感顾问对话。
     *
     * <p>流程：
     * <ol>
     *     <li>history 为空 → 直接返回欢迎语，不消耗 token</li>
     *     <li>选择最终生效的 apiKey / baseUrl / model（前端传入优先）</li>
     *     <li>组装 messages = system 提示词 + 历史消息（按 role 归一为 user/assistant）</li>
     *     <li>POST {baseUrl}/v1/chat/completions，60s 超时</li>
     *     <li>解析 choices[0].message.content；非 2xx 或内容为空 → BizException</li>
     * </ol>
     *
     * @param dto 包含历史消息及可选的 apiKey/baseUrl/model 覆盖
     * @return 情感顾问的回复内容（已 trim）
     * @throws BizException 上游异常、返回为空或网络错误时
     */
    @Override
    public CounselorReplyVO chat(CounselorChatDTO dto) {
        List<CounselorChatDTO.Message> history = dto == null ? null : dto.getMessages();
        if (history == null || history.isEmpty()) {
            CounselorReplyVO vo = new CounselorReplyVO();
            vo.setReply(GREETING);
            return vo;
        }

        // 优先使用前端传入的自定义配置，未传则回退到服务端默认
        String useApiKey = StringUtils.hasText(dto.getApiKey()) ? dto.getApiKey().trim() : apiKey;
        String useBaseUrl = StringUtils.hasText(dto.getBaseUrl()) ? dto.getBaseUrl().trim() : baseUrl;
        String useModel = StringUtils.hasText(dto.getModel()) ? dto.getModel().trim() : model;

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", useModel);
            body.put("temperature", temperature);
            ArrayNode messages = body.putArray("messages");

            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", SYSTEM_PROMPT);

            for (CounselorChatDTO.Message m : history) {
                if (m == null || !StringUtils.hasText(m.getContent())) continue;
                String role = "assistant".equalsIgnoreCase(m.getRole()) ? "assistant" : "user";
                ObjectNode node = messages.addObject();
                node.put("role", role);
                node.put("content", m.getContent());
            }

            // 处理 baseUrl：移除末尾斜杠，如果已包含 /v1 则不再重复添加
            String cleanBaseUrl = useBaseUrl.replaceAll("/+$", "");
            String endpoint = cleanBaseUrl.endsWith("/v1")
                    ? cleanBaseUrl + "/chat/completions"
                    : cleanBaseUrl + "/v1/chat/completions";

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            if (StringUtils.hasText(useApiKey)) {
                reqBuilder.header("Authorization", "Bearer " + useApiKey);
            }
            HttpRequest request = reqBuilder.build();

            log.info("LLM 请求 URL: {}", endpoint);
            log.info("LLM 请求模型: {}", useModel);
            log.debug("LLM 请求 hasApiKey: {}", StringUtils.hasText(useApiKey));

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("LLM 响应 status={}, body={}", resp.statusCode(), resp.body());
            if (resp.statusCode() / 100 != 2) {
                log.error("LLM 调用失败 status={} body={}", resp.statusCode(), resp.body());
                throw new BizException("情感顾问暂时无法回应，请稍后再试 (HTTP " + resp.statusCode() + ")");
            }
            JsonNode root = mapper.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (!StringUtils.hasText(content)) {
                throw new BizException("情感顾问暂时没有回应，请稍后再试");
            }

            CounselorReplyVO vo = new CounselorReplyVO();
            vo.setReply(content.trim());
            return vo;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 LLM 异常", e);
            throw new BizException("情感顾问连接异常：" + e.getMessage());
        }
    }
}
