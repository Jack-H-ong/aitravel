package com.aitravel.service.ai;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * 行程中的单日安排（详细设计 §2.4.2）。
 *
 * @param day 第几天
 * @param theme 当日主题
 * @param spots 当日景点
 * @param lunch 午餐推荐
 * @param dinner 晚餐推荐
 * @param tip 当日提示
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "传输用不可变值对象；List 仅作只读承载，调用方不会修改其内容")
public record TripPlanDay(int day, String theme, List<TripPlanSpot> spots, String lunch,
                          String dinner, String tip) {
}
