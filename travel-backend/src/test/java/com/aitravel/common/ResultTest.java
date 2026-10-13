package com.aitravel.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/** {@link Result} 单元测试（§2.1.4）。 */
class ResultTest {

    @Test
    void okWithData() {
        String data = "payload";
        Result<String> result = Result.ok(data);

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertSame(data, result.getData());
    }

    @Test
    void okWithNull() {
        Result<Object> result = Result.ok(null);

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithErrorCode() {
        Result<Void> result = Result.fail(ErrorCode.NOT_FOUND);

        assertEquals(404, result.getCode());
        assertEquals("资源不存在", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithCustomMessage() {
        Result<Void> result = Result.fail(ErrorCode.BAD_REQUEST, ErrorMessage.USERNAME_EXISTS);

        assertEquals(400, result.getCode());
        assertEquals("用户名已注册", result.getMessage());
        assertNull(result.getData());
    }
}
