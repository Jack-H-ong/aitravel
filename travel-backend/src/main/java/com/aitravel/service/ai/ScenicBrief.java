package com.aitravel.service.ai;

import java.math.BigDecimal;

/**
 * 用于 Prompt 构建的景点简要信息（与持久化实体解耦，详细设计 §2.4.2）。
 *
 * @param id 景点 ID
 * @param name 景点名称
 * @param city 所在城市
 * @param category 类别
 * @param tags 标签
 * @param description 简介
 * @param openTime 开放时间
 * @param price 门票价格
 */
public record ScenicBrief(Long id, String name, String city, String category, String tags,
                          String description, String openTime, BigDecimal price) {
}
