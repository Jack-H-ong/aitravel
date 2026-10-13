package com.aitravel.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * AI 响应解析与容错（详细设计 §2.4.5）。
 *
 * <p>解析流程：剥离 Markdown 围栏 → 截取首个 {@code {}} 到末个 {@code }} 的子串 →
 * Jackson 反序列化 → 字段缺失给默认值（{@code stayMinutes=60}、字符串为空串）→
 * {@code days} 为空或长度与请求不符时抛 {@link AiServiceException}。
 */
@Component
public class AiResponseParser {

    /** 景点默认停留分钟数。 */
    private static final int DEFAULT_STAY_MINUTES = 60;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 解析行程 JSON。
     *
     * <p>相对详细设计 §2.4.1 的类图，本方法额外接收 {@code expectedDays} 以便校验天数，
     * 调用方 {@code TongyiAiClient} 传入 {@code request.days()}。
     *
     * @param raw 大模型原始输出
     * @param expectedDays 请求的期望天数（≤0 时不校验）
     * @return 结构化行程
     * @throws AiServiceException 无法解析或天数不符
     */
    public TripPlan parseTripPlan(String raw, int expectedDays) {
        String json = extractJson(raw);
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("无法解析 AI 返回的行程 JSON: " + e.getMessage());
        }
        if (root == null || !root.isObject()) {
            throw new AiServiceException("AI 返回的行程内容不是合法 JSON 对象");
        }
        String title = text(root, "title");
        JsonNode daysNode = root.get("days");
        if (daysNode == null || !daysNode.isArray() || daysNode.isEmpty()) {
            throw new AiServiceException("AI 返回的行程缺少 days 字段");
        }
        if (expectedDays > 0 && daysNode.size() != expectedDays) {
            throw new AiServiceException(
                    "AI 返回的行程天数与请求不符: 期望 " + expectedDays + "，实际 " + daysNode.size());
        }
        List<TripPlanDay> days = new ArrayList<>();
        int index = 0;
        for (JsonNode dayNode : daysNode) {
            index++;
            days.add(parseDay(dayNode, index));
        }
        return new TripPlan(title, days);
    }

    /**
     * 剥离 Markdown 代码围栏（```` ```json ... ``` ````）。
     *
     * @param raw 原始文本
     * @return 去除围栏后的文本
     */
    public String stripCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String content = raw.strip();
        if (!content.startsWith("```")) {
            return content;
        }
        int firstNewline = content.indexOf('\n');
        if (firstNewline >= 0) {
            content = content.substring(firstNewline + 1);
        } else {
            content = content.substring(3);
        }
        int closingFence = content.lastIndexOf("```");
        if (closingFence >= 0) {
            content = content.substring(0, closingFence);
        }
        return content.strip();
    }

    /**
     * 剥离围栏并截取首个 {@code {} 到末个 {@code }} 的 JSON 子串。
     *
     * @param raw 原始文本
     * @return JSON 子串
     * @throws AiServiceException 未找到 JSON 对象
     */
    private String extractJson(String raw) {
        String stripped = stripCodeFence(raw);
        int start = stripped.indexOf('{');
        int end = stripped.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new AiServiceException("AI 返回内容中未找到 JSON 对象");
        }
        return stripped.substring(start, end + 1);
    }

    /**
     * 解析单日安排。
     *
     * @param node 单日 JSON 节点
     * @param fallbackDay 缺失 day 字段时的默认天数
     * @return 单日安排
     * @throws AiServiceException 节点不是 JSON 对象
     */
    private TripPlanDay parseDay(JsonNode node, int fallbackDay) {
        if (!node.isObject()) {
            throw new AiServiceException("AI 返回的 day 元素不是 JSON 对象");
        }
        int day = intOrDefault(node, "day", fallbackDay);
        String theme = text(node, "theme");
        List<TripPlanSpot> spots = new ArrayList<>();
        JsonNode spotsNode = node.get("spots");
        if (spotsNode != null && spotsNode.isArray()) {
            int order = 0;
            for (JsonNode spotNode : spotsNode) {
                order++;
                spots.add(parseSpot(spotNode, order));
            }
        }
        return new TripPlanDay(day, theme, spots, text(node, "lunch"),
                text(node, "dinner"), text(node, "tip"));
    }

    /**
     * 解析单个景点安排，缺失字段给默认值。
     *
     * @param node 景点 JSON 节点
     * @param fallbackOrder 缺失 order 字段时的默认顺序
     * @return 景点安排
     */
    private TripPlanSpot parseSpot(JsonNode node, int fallbackOrder) {
        String name = text(node, "name");
        String time = text(node, "time");
        int order = intOrDefault(node, "order", fallbackOrder);
        int stayMinutes = intOrDefault(node, "stayMinutes", DEFAULT_STAY_MINUTES);
        return new TripPlanSpot(name, time, order, stayMinutes, text(node, "tip"));
    }

    /**
     * 读取整数字段，缺失或不可转换时返回默认值。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @param defaultValue 默认值
     * @return 字段值或默认值
     */
    private int intOrDefault(JsonNode node, String field, int defaultValue) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.canConvertToInt()) {
            return defaultValue;
        }
        return value.asInt();
    }

    /**
     * 读取字符串字段，缺失或为 null 时返回空串。
     *
     * @param node JSON 节点
     * @param field 字段名
     * @return 字段值或空串
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
