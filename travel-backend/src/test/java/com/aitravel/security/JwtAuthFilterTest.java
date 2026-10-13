package com.aitravel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** {@link JwtAuthFilter} 单元测试（§2.3.4 / §2.3.8，只解析不裁决）。 */
class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void validTokenPopulatesContextDuringChain() throws Exception {
        String token = jwtUtil.generateToken(9L, "alice", "admin");
        LoginUser[] captured = new LoginUser[1];

        filter.doFilterInternal(requestWithHeader("Bearer " + token), new MockHttpServletResponse(),
                capturingChain(captured));

        assertNotNull(captured[0]);
        assertEquals(9L, captured[0].getUserId());
        assertEquals("alice", captured[0].getUsername());
        assertTruthy(captured[0].isAdmin());
        assertNull(UserContext.get(), "请求结束后必须 clear");
    }

    @Test
    void missingTokenLeavesContextEmptyButPassesThrough() throws Exception {
        LoginUser[] captured = new LoginUser[1];
        boolean[] invoked = new boolean[1];

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(),
                captureAndFlag(captured, invoked));

        assertTrue(invoked[0], "过滤器应放行到 chain");
        assertNull(captured[0]);
    }

    @Test
    void invalidTokenIsIgnoredNotRejected() throws Exception {
        boolean[] invoked = new boolean[1];
        LoginUser[] captured = new LoginUser[1];

        filter.doFilterInternal(requestWithHeader("Bearer not-a-jwt"), new MockHttpServletResponse(),
                captureAndFlag(captured, invoked));

        assertTrue(invoked[0], "无效 Token 不应阻断请求");
        assertNull(captured[0]);
    }

    @Test
    void contextClearedEvenWhenChainThrows() {
        String token = jwtUtil.generateToken(5L, "bob", "user");
        FilterChain exploding = (req, resp) -> {
            throw new IllegalStateException("boom");
        };

        try {
            filter.doFilterInternal(requestWithHeader("Bearer " + token),
                    new MockHttpServletResponse(), exploding);
        } catch (Exception expected) {
            // 期望异常；本用例只验证 finally 中的 clear
        }

        assertNull(UserContext.get(), "异常路径也必须 clear");
    }

    private MockHttpServletRequest requestWithHeader(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", value);
        return request;
    }

    private FilterChain capturingChain(LoginUser[] captured) {
        return (req, resp) -> captured[0] = UserContext.get();
    }

    private FilterChain captureAndFlag(LoginUser[] captured, boolean[] invoked) {
        return (req, resp) -> {
            invoked[0] = true;
            captured[0] = UserContext.get();
        };
    }

    private void assertTruthy(boolean value) {
        assertTrue(value);
    }
}
