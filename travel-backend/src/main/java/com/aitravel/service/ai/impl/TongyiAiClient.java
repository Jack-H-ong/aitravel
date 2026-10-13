package com.aitravel.service.ai.impl;

import com.aitravel.config.AiProperties;
import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiResponseParser;
import com.aitravel.service.ai.AiServiceException;
import com.aitravel.service.ai.ChatContext;
import com.aitravel.service.ai.PromptBuilder;
import com.aitravel.service.ai.TripPlan;
import com.aitravel.service.ai.TripPlanRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 通义千问（DashScope）实现（详细设计 §2.4.3）。
 *
 * <p>通过 Spring 6 同步 {@link RestClient} 调用 DashScope 文本生成接口；
 * 超时/连接失败/4xx/5xx 统一转为 {@link AiServiceException}，<b>不做自动重试</b>。
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 RestClient/AiProperties 由 Spring 容器持有，此处仅保存引用")
public class TongyiAiClient implements AiGateway {

    private static final Logger log = LoggerFactory.getLogger(TongyiAiClient.class);

    private static final String ENDPOINT = "/api/v1/services/aigc/text-generation/generation";
    private static final String SYSTEM_PROMPT = "你是一位专业的旅游规划师。";

    private final RestClient restClient;
    private final AiProperties props;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    /**
     * 构造通义实现。
     *
     * @param restClient 已配置 baseUrl 与超时的 RestClient
     * @param props AI 配置
     * @param promptBuilder Prompt 构建器
     * @param responseParser 响应解析器
     */
    public TongyiAiClient(RestClient restClient, AiProperties props, PromptBuilder promptBuilder,
                          AiResponseParser responseParser) {
        this.restClient = restClient;
        this.props = props;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    @Override
    public TripPlan generateTripPlan(TripPlanRequest request) {
        String prompt = promptBuilder.buildTripPrompt(request);
        String content = callDashScope(prompt, true);
        return responseParser.parseTripPlan(content, request.days());
    }

    @Override
    public String chat(ChatContext context) {
        String prompt = promptBuilder.buildChatPrompt(context);
        return callDashScope(prompt, false);
    }

    /**
     * 调用 DashScope 文本生成接口并提取回答内容。
     *
     * @param prompt 用户 Prompt
     * @param expectJson 是否期望 JSON 输出（仅作语义标记，实际不影响请求体）
     * @return 模型回答内容
     * @throws AiServiceException 调用失败或响应缺失
     */
    private String callDashScope(String prompt, boolean expectJson) {
        log.debug("Calling DashScope, model={}, expectJson={}", props.getModel(), expectJson);
        try {
            DashScopeResponse resp = restClient.post()
                    .uri(ENDPOINT)
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildBody(prompt, expectJson))
                    .retrieve()
                    .body(DashScopeResponse.class);
            return extractContent(resp);
        } catch (RestClientException e) {
            log.warn("AI call failed, model={}, cause={}", props.getModel(), e.toString());
            throw new AiServiceException(e.getMessage());
        }
    }

    /**
     * 构建 DashScope 请求体（详细设计 §2.4.3）。
     *
     * @param prompt 用户 Prompt
     * @param expectJson 是否期望 JSON 输出
     * @return 请求体 Map
     */
    private Map<String, Object> buildBody(String prompt, boolean expectJson) {
        Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
        Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);
        Map<String, Object> input = Map.of("messages", List.of(systemMessage, userMessage));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("result_format", "message");
        parameters.put("temperature", props.getTemperature());
        parameters.put("max_tokens", props.getMaxTokens());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", props.getModel());
        body.put("input", input);
        body.put("parameters", parameters);
        return body;
    }

    /**
     * 从响应中提取 {@code output.choices[0].message.content}。
     *
     * @param resp DashScope 响应
     * @return 回答内容
     * @throws AiServiceException output、choices 或 content 缺失
     */
    private String extractContent(DashScopeResponse resp) {
        if (resp == null || resp.getOutput() == null || resp.getOutput().getChoices() == null
                || resp.getOutput().getChoices().isEmpty()) {
            throw new AiServiceException("AI 服务返回为空：缺少 output.choices");
        }
        Choice choice = resp.getOutput().getChoices().get(0);
        if (choice == null || choice.getMessage() == null
                || choice.getMessage().getContent() == null) {
            throw new AiServiceException("AI 服务返回为空：缺少 message.content");
        }
        return choice.getMessage().getContent();
    }

    /**
     * DashScope 响应根对象。
     */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "仅由 Jackson 反序列化填充后只读访问的响应 DTO")
    public static final class DashScopeResponse {

        private Output output;

        public Output getOutput() {
            return output;
        }

        public void setOutput(Output output) {
            this.output = output;
        }
    }

    /**
     * 响应 output 节点。
     */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "仅由 Jackson 反序列化填充后只读访问的响应 DTO")
    public static final class Output {

        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }
    }

    /**
     * 单个 choice 节点。
     */
    @SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
            justification = "仅由 Jackson 反序列化填充后只读访问的响应 DTO")
    public static final class Choice {

        private Message message;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    /**
     * choice 中的 message 节点。
     */
    public static final class Message {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
