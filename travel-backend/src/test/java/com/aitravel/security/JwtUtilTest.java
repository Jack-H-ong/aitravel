package com.aitravel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

/** {@link JwtUtil} 单元测试（§2.3.8，使用真实 JwtUtil 与测试密钥）。 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";
    private static final long ONE_HOUR = 3_600_000L;

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

    @Test
    void generateThenParseKeepsAllFields() {
        String token = jwtUtil.generateToken(100L, "alice", "admin");

        LoginUser user = jwtUtil.parseToken(token);

        assertEquals(100L, user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals("admin", user.getRole());
        assertTrue(user.isAdmin());
    }

    @Test
    void parseTokenMarksNormalUserAsNotAdmin() {
        String token = jwtUtil.generateToken(7L, "bob", "user");

        LoginUser user = jwtUtil.parseToken(token);

        assertEquals(7L, user.getUserId());
        assertEquals("bob", user.getUsername());
        assertFalse(user.isAdmin());
    }

    @Test
    void isValidTrueForFreshToken() {
        assertTrue(jwtUtil.isValid(jwtUtil.generateToken(1L, "carol", "user")));
    }

    @Test
    void expiredTokenIsInvalidAndParseThrows() {
        JwtUtil expired = new JwtUtil(SECRET, -1L);
        String token = expired.generateToken(1L, "dave", "user");

        assertFalse(jwtUtil.isValid(token));
        assertThrows(JwtException.class, () -> jwtUtil.parseToken(token));
    }

    @Test
    void tamperedSignatureIsInvalid() {
        String token = jwtUtil.generateToken(1L, "eve", "user");
        char flipped = token.charAt(0) == 'a' ? 'b' : 'a';
        String tampered = flipped + token.substring(1);

        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void tokenSignedByAnotherKeyIsInvalid() {
        JwtUtil other = new JwtUtil("another-secret-key-abcdefabcdefabcdefabcdefabcdef", ONE_HOUR);
        String foreign = other.generateToken(1L, "mallory", "admin");

        assertFalse(jwtUtil.isValid(foreign));
    }

    @Test
    void malformedTokenIsInvalid() {
        assertFalse(jwtUtil.isValid("not-a-jwt"));
    }
}
