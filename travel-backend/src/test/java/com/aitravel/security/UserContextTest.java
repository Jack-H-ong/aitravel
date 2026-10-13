package com.aitravel.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** {@link UserContext} 单元测试（§2.3.3）。 */
class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void emptyContextReturnsNull() {
        assertNull(UserContext.get());
        assertNull(UserContext.getUserId());
        assertFalse(UserContext.isAdmin());
    }

    @Test
    void setGetAndUserId() {
        UserContext.set(new LoginUser(42L, "alice", "user"));

        assertEquals(42L, UserContext.getUserId());
        assertEquals("alice", UserContext.get().getUsername());
        assertFalse(UserContext.isAdmin());
    }

    @Test
    void adminDetected() {
        UserContext.set(new LoginUser(1L, "root", "admin"));

        assertTrue(UserContext.isAdmin());
    }

    @Test
    void clearRemovesBinding() {
        UserContext.set(new LoginUser(42L, "alice", "user"));

        UserContext.clear();

        assertNull(UserContext.get());
        assertNull(UserContext.getUserId());
    }
}
