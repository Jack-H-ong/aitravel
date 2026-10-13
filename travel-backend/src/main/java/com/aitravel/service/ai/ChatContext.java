package com.aitravel.service.ai;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * AI 问答上下文（详细设计 §2.4.2）。
 *
 * @param tripSummary 当前行程摘要（可为空）
 * @param scenicSummary 目的地景点摘要（可为空）
 * @param history 历史对话（由 M-CHAT 预裁剪）
 * @param question 用户问题
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "传输用不可变值对象；history 仅作只读承载，调用方不会修改其内容")
public record ChatContext(String tripSummary, String scenicSummary, List<ChatTurn> history,
                          String question) {
}
