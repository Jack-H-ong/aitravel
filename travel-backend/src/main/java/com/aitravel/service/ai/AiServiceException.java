package com.aitravel.service.ai;

/**
 * AI 服务异常。
 *
 * <p>规范归属 M-AI 模块（详细设计 §2.4.1）。因 M-COMMON 先于 M-AI 实现，此处的定义由
 * {@code GlobalExceptionHandler} 引用以打通编译；后续 M-AI 模块直接复用本类，不再新建。
 * 由 {@code GlobalExceptionHandler} 统一转为错误码 5001。
 */
public class AiServiceException extends RuntimeException {

    private final String detail;

    /**
     * 构造 AI 服务异常。
     *
     * @param detail 详细失败原因，同时作为异常 message
     */
    public AiServiceException(String detail) {
        super(detail);
        this.detail = detail;
    }

    /**
     * 返回详细失败原因（仅用于日志，不直接返回给客户端）。
     *
     * @return 详细原因
     */
    public String getDetail() {
        return detail;
    }
}
