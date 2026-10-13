package com.aitravel.service.ai;

/**
 * 行程中的单个景点安排（详细设计 §2.4.2）。
 *
 * @param name 景点名称
 * @param time 建议游玩时间
 * @param order 顺序
 * @param stayMinutes 停留分钟数（缺失时默认 60）
 * @param tip 游玩提示
 */
public record TripPlanSpot(String name, String time, int order, int stayMinutes, String tip) {
}
