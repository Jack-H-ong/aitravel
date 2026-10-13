package com.aitravel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.security.annotation.OptionalAuthApi;
import com.aitravel.security.annotation.PublicApi;
import com.aitravel.security.annotation.RequireAdmin;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

/** {@link AuthInterceptor} 单元测试（§2.3.5 / §2.3.8）。 */
class AuthInterceptorTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
    private final AuthInterceptor interceptor = new AuthInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ---------- 非 HandlerMethod ----------

    @Test
    void nonHandlerMethodIsAllowed() throws Exception {
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    // ---------- @PublicApi ----------

    @Test
    void publicApiAllowsWithoutIdentity() throws Exception {
        assertTrue(interceptor.preHandle(request, response, handler(new MixedFixture(), "publicApi")));
        assertNull(UserContext.get());
    }

    @Test
    void publicApiWithValidTokenKeepsIdentity() throws Exception {
        String token = jwtUtil.generateToken(11L, "alice", "user");

        throughFilter("Bearer " + token, (req, resp) -> {
            assertTrue(interceptor.preHandle(request, response,
                    handler(new MixedFixture(), "publicApi")));
            assertNotNull(UserContext.get(), "过滤器应先填充身份");
            assertEquals(11L, UserContext.getUserId());
        });
    }

    @Test
    void classLevelPublicApiAllowsWithoutIdentity() throws Exception {
        assertTrue(interceptor.preHandle(request, response,
                handler(new PublicClassFixture(), "any")));
    }

    // ---------- 默认：需登录 ----------

    @Test
    void defaultApiWithIdentityIsAllowed() throws Exception {
        UserContext.set(new LoginUser(3L, "bob", "user"));

        assertTrue(interceptor.preHandle(request, response, handler(new MixedFixture(), "plain")));
    }

    @Test
    void defaultApiWithValidTokenIsAllowed() throws Exception {
        String token = jwtUtil.generateToken(3L, "bob", "user");

        throughFilter("Bearer " + token, (req, resp) -> {
            assertTrue(interceptor.preHandle(request, response, handler(new MixedFixture(), "plain")));
            assertEquals(3L, UserContext.getUserId());
        });
    }

    @Test
    void defaultApiWithoutTokenThrowsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response, handler(new MixedFixture(), "plain")));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void expiredTokenOnDefaultApiThrowsUnauthorized() throws Exception {
        JwtUtil expired = new JwtUtil(SECRET, -1L);
        String token = expired.generateToken(4L, "eve", "user");

        throughFilter("Bearer " + token, (req, resp) -> {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> interceptor.preHandle(request, response,
                            handler(new MixedFixture(), "plain")));
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        });
    }

    // ---------- @OptionalAuthApi（D-04）----------

    @Test
    void optionalAuthWithoutTokenAllowsAndLeavesContextEmpty() throws Exception {
        assertTrue(interceptor.preHandle(request, response, handler(new MixedFixture(), "optionalAuth")));
        assertNull(UserContext.get());
    }

    @Test
    void optionalAuthWithInvalidTokenAllowsAndLeavesContextEmpty() throws Exception {
        throughFilter("Bearer not-a-jwt", (req, resp) -> {
            assertTrue(interceptor.preHandle(request, response,
                    handler(new MixedFixture(), "optionalAuth")));
            assertNull(UserContext.get());
        });
    }

    @Test
    void optionalAuthWithValidTokenPopulatesIdentity() throws Exception {
        String token = jwtUtil.generateToken(8L, "carol", "user");

        throughFilter("Bearer " + token, (req, resp) -> {
            assertTrue(interceptor.preHandle(request, response,
                    handler(new MixedFixture(), "optionalAuth")));
            assertEquals(8L, UserContext.getUserId());
        });
    }

    // ---------- @RequireAdmin ----------

    @Test
    void requireAdminWithAdminIdentityIsAllowed() throws Exception {
        String token = jwtUtil.generateToken(1L, "root", "admin");

        throughFilter("Bearer " + token, (req, resp) -> {
            assertTrue(interceptor.preHandle(request, response,
                    handler(new MixedFixture(), "requireAdmin")));
        });
    }

    @Test
    void requireAdminWithoutIdentityThrowsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response,
                        handler(new MixedFixture(), "requireAdmin")));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    void requireAdminWithNormalUserThrowsForbidden() {
        UserContext.set(new LoginUser(2L, "bob", "user"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response,
                        handler(new MixedFixture(), "requireAdmin")));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void classLevelRequireAdminForbidsNormalUser() {
        UserContext.set(new LoginUser(2L, "bob", "user"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, response,
                        handler(new AdminClassFixture(), "adminOnly")));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ---------- 方法级优先于类级 ----------

    @Test
    void methodLevelPublicApiOverridesClassLevelAdmin() throws Exception {
        assertTrue(interceptor.preHandle(request, response,
                handler(new MethodOverrideFixture(), "openForAll")));
    }

    @Test
    void methodLevelOptionalAuthOverridesClassLevelAdmin() throws Exception {
        assertTrue(interceptor.preHandle(request, response,
                handler(new MethodOverrideFixture(), "optionalInAdminClass")));
    }

    private HandlerMethod handler(Object bean, String methodName) {
        try {
            return new HandlerMethod(bean, methodName);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("fixture 方法不存在: " + methodName, e);
        }
    }

    private void throughFilter(String headerValue, FilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        if (headerValue != null) {
            req.addHeader("Authorization", headerValue);
        }
        filter.doFilterInternal(req, response, chain);
    }

    // ---------- fixtures ----------

    /** 方法级注解混合的控制器夹具。 */
    static class MixedFixture {

        @PublicApi
        public void publicApi() {
        }

        @OptionalAuthApi
        public void optionalAuth() {
        }

        @RequireAdmin
        public void requireAdmin() {
        }

        public void plain() {
        }
    }

    /** 类级 @PublicApi 夹具。 */
    @PublicApi
    static class PublicClassFixture {

        public void any() {
        }
    }

    /** 类级 @RequireAdmin 夹具。 */
    @RequireAdmin
    static class AdminClassFixture {

        public void adminOnly() {
        }
    }

    /** 类级 @RequireAdmin、方法级更宽松注解的夹具（验证方法级优先）。 */
    @RequireAdmin
    static class MethodOverrideFixture {

        @PublicApi
        public void openForAll() {
        }

        @OptionalAuthApi
        public void optionalInAdminClass() {
        }
    }
}
