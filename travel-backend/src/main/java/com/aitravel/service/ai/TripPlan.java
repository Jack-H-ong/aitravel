package com.aitravel.service.ai;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * AI 生成的结构化行程（详细设计 §2.4.2）。
 *
 * @param title 行程标题
 * @param days 每日安排
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "传输用不可变值对象；List 仅作只读承载，调用方不会修改其内容")
public record TripPlan(String title, List<TripPlanDay> days) {
}
