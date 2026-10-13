package com.aitravel.common;

/**
 * 统一响应体。成功固定 {@code code=200}、{@code message="success"}。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    /** Jackson 反序列化用无参构造。 */
    public Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造成功的空响应体。
     *
     * @param <T> 业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 构造携带数据的成功响应体。
     *
     * @param data 业务数据，可为 {@code null}
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 以错误码默认文案构造失败响应体。
     *
     * @param errorCode 错误码
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 以自定义文案构造失败响应体（文案为空时回退到错误码默认文案）。
     *
     * @param errorCode 错误码
     * @param message   自定义文案，可为 {@code null}
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        String actual = (message == null || message.isBlank()) ? errorCode.getMessage() : message;
        return new Result<>(errorCode.getCode(), actual, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
