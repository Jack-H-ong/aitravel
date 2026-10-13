package com.aitravel.service.ai;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Prompt 构建器（详细设计 §2.4.4）。
 *
 * <p>持有需求文档 3.3.1（生成攻略）与 3.4.1（智能问答）模板，并做上下文压缩：
 * 景点简介截断 100 字、偏好以中文顿号连接、历史对话按角色格式化，避免 Prompt 超长。
 */
@Component
public class PromptBuilder {

    /** 景点简介截断长度。 */
    private static final int DESCRIPTION_MAX = 100;

    /**
     * 构建生成行程的 Prompt（需求 3.3.1 模板）。
     *
     * @param request 生成请求
     * @return Prompt 文本
     */
    public String buildTripPrompt(TripPlanRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的旅游规划师。请根据以下信息为用户生成一份旅游攻略：\n");
        sb.append("- 目的地：").append(blankToDash(request.city())).append('\n');
        sb.append("- 旅行天数：").append(request.days()).append('天')
                .append(nights(request.days())).append("晚\n");
        sb.append("- 预算范围：").append(blankToDash(request.budget())).append('\n');
        sb.append("- 消费水平：").append(blankToDash(request.spendMode())).append('\n');
        sb.append("- 兴趣偏好：").append(joinPreferences(request.preferences())).append('\n');
        sb.append("\n以下是目的地相关的景点信息：\n");
        sb.append(buildScenicContext(request.scenicList())).append('\n');
        sb.append("\n请返回 JSON 格式，结构如下：\n");
        sb.append("{\n");
        sb.append("  \"title\": \"行程标题\",\n");
        sb.append("  \"days\": [\n");
        sb.append("    {\n");
        sb.append("      \"day\": 1,\n");
        sb.append("      \"theme\": \"当日主题\",\n");
        sb.append("      \"spots\": [\n");
        sb.append("        {\n");
        sb.append("          \"name\": \"景点名\",\n");
        sb.append("          \"time\": \"建议游玩时间\",\n");
        sb.append("          \"order\": 1,\n");
        sb.append("          \"stayMinutes\": 120,\n");
        sb.append("          \"tip\": \"游玩提示\"\n");
        sb.append("        }\n");
        sb.append("      ],\n");
        sb.append("      \"lunch\": \"午餐推荐\",\n");
        sb.append("      \"dinner\": \"晚餐推荐\",\n");
        sb.append("      \"tip\": \"当日提示\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * 构建问答的 Prompt（需求 3.4.1 模板）。
     *
     * @param context 问答上下文
     * @return Prompt 文本
     */
    public String buildChatPrompt(ChatContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的旅游顾问，擅长为用户提供旅行建议。请根据以下信息回答用户的问题：\n");
        sb.append("\n用户当前行程（如有）：\n");
        sb.append(blankToPlaceholder(context.tripSummary())).append('\n');
        sb.append("\n目的地景点数据：\n");
        sb.append(blankToPlaceholder(context.scenicSummary())).append('\n');
        sb.append("\n历史对话：\n");
        sb.append(formatHistory(context.history())).append('\n');
        sb.append("\n用户问题：").append(blankToDash(context.question())).append('\n');
        sb.append("\n请简洁、实用地回答用户问题，适当推荐景点和美食。\n");
        return sb.toString();
    }

    /**
     * 压缩景点列表为上下文文本。
     *
     * @param scenicList 景点简要信息
     * @return 压缩后的文本（无数据时返回占位符）
     */
    private String buildScenicContext(List<ScenicBrief> scenicList) {
        if (scenicList == null || scenicList.isEmpty()) {
            return "（暂无景点数据）";
        }
        StringBuilder sb = new StringBuilder();
        for (ScenicBrief scenic : scenicList) {
            sb.append("- 名称：").append(blankToDash(scenic.name()))
                    .append("；类别：").append(blankToDash(scenic.category()))
                    .append("；标签：").append(blankToDash(scenic.tags()))
                    .append("；开放时间：").append(blankToDash(scenic.openTime()))
                    .append("；价格：").append(formatPrice(scenic.price()))
                    .append("；简介：").append(truncate(scenic.description()))
                    .append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 以中文顿号连接偏好标签。
     *
     * @param preferences 偏好标签
     * @return 连接后的文本（空时返回「无」）
     */
    private String joinPreferences(List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return "无";
        }
        return String.join("、", preferences);
    }

    /**
     * 格式化历史对话。
     *
     * @param history 历史对话
     * @return 每行「角色：内容」（空时返回占位符）
     */
    private String formatHistory(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatTurn turn : history) {
            sb.append(roleLabel(turn.role())).append('：')
                    .append(blankToDash(turn.content())).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 将角色标识转为中文标签。
     *
     * @param role 角色标识
     * @return 中文标签（未知角色原样返回）
     */
    private String roleLabel(String role) {
        if (role == null) {
            return "未知";
        }
        return switch (role) {
            case "user" -> "用户";
            case "assistant" -> "助手";
            case "system" -> "系统";
            default -> role;
        };
    }

    /**
     * 截断景点简介，避免 Prompt 超长。
     *
     * @param description 原始简介
     * @return 截断后的简介（超过 100 字时追加省略号）
     */
    private String truncate(String description) {
        if (description == null) {
            return "";
        }
        return description.length() <= DESCRIPTION_MAX
                ? description
                : description.substring(0, DESCRIPTION_MAX) + "…";
    }

    /**
     * 格式化价格。
     *
     * @param price 价格
     * @return 价格文本（为空时返回「未知」）
     */
    private String formatPrice(BigDecimal price) {
        return price == null ? "未知" : price.toPlainString();
    }

    /**
     * 天数转晚数（天数减一，至少为零）。
     *
     * @param days 天数
     * @return 晚数
     */
    private int nights(int days) {
        return Math.max(days - 1, 0);
    }

    /**
     * 空值转破折号占位。
     *
     * @param value 原始值
     * @return 非空值或「-」
     */
    private String blankToDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    /**
     * 空值转「（无）」占位。
     *
     * @param value 原始值
     * @return 非空值或「（无）」
     */
    private String blankToPlaceholder(String value) {
        return isBlank(value) ? "（无）" : value;
    }

    /**
     * 判断字符串是否为空或空白。
     *
     * @param value 原始值
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
