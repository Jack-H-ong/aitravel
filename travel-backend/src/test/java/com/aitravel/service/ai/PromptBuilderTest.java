package com.aitravel.service.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link PromptBuilder} 单元测试（详细设计 §2.4.4）。 */
class PromptBuilderTest {

    private final PromptBuilder builder = new PromptBuilder();

    private static final String LONG_DESCRIPTION = "西湖".repeat(80);

    @Test
    void tripPromptContainsAllInputs() {
        TripPlanRequest request = new TripPlanRequest("杭州", 3, "5000元以内", "舒适",
                List.of("自然", "美食", "历史"),
                List.of(new ScenicBrief(1L, "西湖", "杭州", "自然景观", "湖景/夜景",
                        "著名的淡水湖", "全天", new BigDecimal("0"))));

        String prompt = builder.buildTripPrompt(request);

        assertTrue(prompt.contains("你是一位专业的旅游规划师"));
        assertTrue(prompt.contains("目的地：杭州"));
        assertTrue(prompt.contains("旅行天数：3天2晚"));
        assertTrue(prompt.contains("预算范围：5000元以内"));
        assertTrue(prompt.contains("消费水平：舒适"));
        assertTrue(prompt.contains("兴趣偏好：自然、美食、历史"));
        assertTrue(prompt.contains("名称：西湖"));
        assertTrue(prompt.contains("开放时间：全天"));
        assertTrue(prompt.contains("价格：0"));
        assertTrue(prompt.contains("\"title\""));
        assertTrue(prompt.contains("\"days\""));
    }

    @Test
    void tripPromptTruncatesLongDescription() {
        TripPlanRequest request = new TripPlanRequest("杭州", 1, "1000", "经济",
                List.of("自然"),
                List.of(new ScenicBrief(1L, "西湖", "杭州", "自然", "湖景",
                        LONG_DESCRIPTION, "全天", new BigDecimal("10"))));

        String prompt = builder.buildTripPrompt(request);

        assertTrue(prompt.contains("西湖".repeat(50) + "…"));
        assertFalse(prompt.contains(LONG_DESCRIPTION));
    }

    @Test
    void tripPromptHandlesNoScenicData() {
        TripPlanRequest request = new TripPlanRequest("杭州", 1, "1000", "经济",
                List.of(), List.of());

        String prompt = builder.buildTripPrompt(request);

        assertTrue(prompt.contains("（暂无景点数据）"));
        assertTrue(prompt.contains("兴趣偏好：无"));
    }

    @Test
    void chatPromptContainsContextAndHistory() {
        ChatContext context = new ChatContext(
                "杭州3天行程",
                "- 名称：西湖；类别：自然",
                List.of(new ChatTurn("user", "我想去西湖"),
                        new ChatTurn("assistant", "推荐早上游览")),
                "附近有什么好吃的？");

        String prompt = builder.buildChatPrompt(context);

        assertTrue(prompt.contains("你是一位专业的旅游顾问"));
        assertTrue(prompt.contains("杭州3天行程"));
        assertTrue(prompt.contains("- 名称：西湖；类别：自然"));
        assertTrue(prompt.contains("用户：我想去西湖"));
        assertTrue(prompt.contains("助手：推荐早上游览"));
        assertTrue(prompt.contains("用户问题：附近有什么好吃的？"));
    }

    @Test
    void chatPromptHandlesEmptyContext() {
        ChatContext context = new ChatContext(null, "", List.of(), "怎么安排？");

        String prompt = builder.buildChatPrompt(context);

        assertTrue(prompt.contains("用户当前行程（如有）：\n（无）"));
        assertTrue(prompt.contains("（无）"));
        assertTrue(prompt.contains("历史对话：\n（无）"));
        assertTrue(prompt.contains("用户问题：怎么安排？"));
    }
}
