package com.aitravel.common;

/**
 * 全局错误码枚举。每个枚举值携带对外的 {@code code} 与默认 {@code message}。
 */
public enum ErrorCode {

    /** 成功。 */
    SUCCESS(200, "success"),

    /** 参数校验失败、业务规则失败。 */
    BAD_REQUEST(400, "请求参数错误"),

    /** Token 缺失/无效/过期、账号被禁用。 */
    UNAUTHORIZED(401, "未认证"),

    /** 非 admin 访问管理接口、越权访问他人资源。 */
    FORBIDDEN(403, "无权限"),

    /** 景点/行程不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 未捕获异常。 */
    INTERNAL_ERROR(500, "服务器内部错误"),

    /** 大模型超时/报错/响应不可解析。 */
    AI_SERVICE_ERROR(5001, "AI 服务异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 返回错误码数值。
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 返回错误码默认文案。
     *
     * @return 默认文案
     */
    public String getMessage() {
        return message;
    }
}
