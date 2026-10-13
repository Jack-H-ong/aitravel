package com.aitravel.service.ai;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * 生成行程的请求参数（详细设计 §2.4.2）。
 *
 * @param city 目的地城市
 * @param days 旅行天数
 * @param budget 预算范围
 * @param spendMode 消费水平（经济/舒适/高端）
 * @param preferences 兴趣偏好标签
 * @param scenicList 目的地相关景点简要信息
 */
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "上游仅传只读列表，值对象不修改也不外泄其内部表示")
public record TripPlanRequest(String city, int days, String budget, String spendMode,
                              List<String> preferences, List<ScenicBrief> scenicList) {
}
