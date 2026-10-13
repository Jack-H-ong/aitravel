package com.aitravel.common;

import com.aitravel.service.ai.AiServiceException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。将各类异常统一转换为 {@link Result}，保证响应体不泄露堆栈、SQL、类名。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 {@code @Valid} 校验失败（请求体）：400 + 首个字段错误信息。
     *
     * @param ex 校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        return Result.fail(ErrorCode.BAD_REQUEST, firstFieldError(ex.getBindingResult()));
    }

    /**
     * 处理表单/参数绑定失败：400 + 首个字段错误信息。
     *
     * @param ex 绑定异常
     * @return 失败响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        return Result.fail(ErrorCode.BAD_REQUEST, firstFieldError(ex.getBindingResult()));
    }

    /**
     * 处理路径/请求参数类型不匹配（如非数字 {@code {id}}）：400「请求参数错误」。
     *
     * @param ex 参数类型不匹配异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Method argument type mismatch: {}", ex.getName());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理请求体缺失或 JSON 格式错误：400「请求参数错误」。
     *
     * @param ex 报文不可读异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Request body not readable: {}", ex.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理缺少必填请求参数：400「请求参数错误」。
     *
     * @param ex 缺少请求参数异常
     * @return 失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理 {@code @Validated} 方法参数校验失败：400「请求参数错误」。
     *
     * @param ex 约束校验异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理业务异常：按错误码返回，文案取异常 message。
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 处理 AI 服务异常：统一转换为 5001，具体原因仅记录日志。
     *
     * @param ex AI 服务异常
     * @return 失败响应
     */
    @ExceptionHandler(AiServiceException.class)
    public Result<Void> handleAi(AiServiceException ex) {
        log.warn("AI service error: {}", ex.getDetail());
        return Result.fail(ErrorCode.AI_SERVICE_ERROR);
    }

    /**
     * 处理请求 Content-Type 不支持（如 form 表单提交到 JSON 接口）：400「请求参数错误」。
     *
     * @param ex 媒体类型不支持异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理请求方法不支持（如对 POST 接口发 GET）：400「请求参数错误」。
     *
     * @param ex 请求方法不支持异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Request method not supported: {}", ex.getMethod());
        return Result.fail(ErrorCode.BAD_REQUEST);
    }

    /**
     * 处理请求路径无映射（404）：返回「资源不存在」，不暴露具体路径。
     *
     * @param ex 静态资源未找到异常
     * @return 失败响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getResourcePath());
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    /**
     * 兜底处理未捕获异常：记录完整堆栈到日志，响应体仅含泛化文案。
     *
     * @param ex 未捕获异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }

    /**
     * 取校验结果中的首个字段错误信息；取不到时回退到 {@link ErrorCode#BAD_REQUEST} 文案。
     */
    private String firstFieldError(BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasFieldErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }
        return ErrorCode.BAD_REQUEST.getMessage();
    }
}
