package com.aitravel.common;

/**
 * 业务异常。携带 {@link ErrorCode}，message 默认取错误码文案，可被自定义文案覆盖。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 使用错误码默认文案构造。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义文案构造（文案为空时回退到错误码默认文案）。
     *
     * @param errorCode 错误码
     * @param message   自定义文案，可为 {@code null}
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super((message == null || message.isBlank()) ? errorCode.getMessage() : message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
