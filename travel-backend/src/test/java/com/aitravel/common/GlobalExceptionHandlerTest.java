package com.aitravel.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aitravel.service.ai.AiServiceException;
import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** {@link GlobalExceptionHandler} 单元测试（§2.1.4）。 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationReturnsFirstErrorOnly() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "username", "target", false, null, null,
                ErrorMessage.USERNAME_EXISTS));
        bindingResult.addError(new FieldError("target", "password", "target", false, null, null,
                ErrorMessage.PASSWORD_NOT_MATCH));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter(),
                bindingResult);

        Result<Void> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertNotNull(result.getMessage());
        assertEquals(bindingResult.getFieldError().getDefaultMessage(), result.getMessage());
        assertTrue(ErrorMessage.USERNAME_EXISTS.equals(result.getMessage())
                || ErrorMessage.PASSWORD_NOT_MATCH.equals(result.getMessage()));
    }

    @Test
    void handleBusinessUsesErrorCodeAndMessage() {
        BusinessException ex =
                new BusinessException(ErrorCode.NOT_FOUND, ErrorMessage.SCENIC_NOT_FOUND);

        Result<Void> result = handler.handleBusiness(ex);

        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMessage());
    }

    @Test
    void handleAiReturns5001() {
        Result<Void> result = handler.handleAi(new AiServiceException("boom"));

        assertEquals(5001, result.getCode());
        assertEquals(ErrorCode.AI_SERVICE_ERROR.getMessage(), result.getMessage());
    }

    @Test
    void handleUnknownHidesExceptionDetails() {
        Result<Void> result = handler.handleUnknown(new NullPointerException("secret detail"));

        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误", result.getMessage());
        assertFalse(result.getMessage().contains("NullPointerException"));
        assertFalse(result.getMessage().contains("secret detail"));
    }

    @Test
    void handleTypeMismatchReturns400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "favorite", Long.class, "id", null, new NumberFormatException("favorite"));

        Result<Void> result = handler.handleTypeMismatch(ex);

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
        assertFalse(result.getMessage().contains("favorite"));
    }

    @Test
    void handleNotReadableReturns400() {
        Result<Void> result =
                handler.handleNotReadable(new HttpMessageNotReadableException("bad json"));

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
        assertFalse(result.getMessage().contains("bad json"));
    }

    @Test
    void handleMissingParamReturns400() {
        Result<Void> result = handler.handleMissingParam(
                new MissingServletRequestParameterException("size", "int"));

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void handleConstraintViolationReturns400() {
        ConstraintViolationException ex =
                new ConstraintViolationException("must not be null", null);

        Result<Void> result = handler.handleConstraintViolation(ex);

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
        assertFalse(result.getMessage().contains("must not be null"));
    }

    @Test
    void handleMediaTypeNotSupportedReturns400() {
        HttpMediaTypeNotSupportedException ex =
                new HttpMediaTypeNotSupportedException(MediaType.APPLICATION_FORM_URLENCODED_VALUE);

        Result<Void> result = handler.handleMediaTypeNotSupported(ex);

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void handleMethodNotSupportedReturns400() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("DELETE");

        Result<Void> result = handler.handleMethodNotSupported(ex);

        assertEquals(400, result.getCode());
        assertEquals(ErrorCode.BAD_REQUEST.getMessage(), result.getMessage());
    }

    @Test
    void handleNoResourceReturns404() {
        NoResourceFoundException ex = new NoResourceFoundException(null, "api/login");

        Result<Void> result = handler.handleNoResource(ex);

        assertEquals(404, result.getCode());
        assertEquals(ErrorCode.NOT_FOUND.getMessage(), result.getMessage());
        assertFalse(result.getMessage().contains("api/login"));
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummy(String arg) {
    }
}
