package com.aitravel.service.ai;

/**
 * AI 服务网关：模块对外唯一契约（详细设计 §2.4.2）。
 *
 * <p>调用方（M-TRIP / M-CHAT）只依赖本接口与中间 record，不依赖任何 Provider 报文结构。
 * 更换 Provider 时调用方零改动。
 */
public interface AiGateway {

    /**
     * 生成结构化行程。
     *
     * @param request 生成请求
     * @return 结构化行程
     * @throws AiServiceException 调用或解析失败
     */
    TripPlan generateTripPlan(TripPlanRequest request);

    /**
     * 生成问答回答文本。
     *
     * @param context 问答上下文
     * @return 回答文本
     * @throws AiServiceException 调用失败
     */
    String chat(ChatContext context);
}
