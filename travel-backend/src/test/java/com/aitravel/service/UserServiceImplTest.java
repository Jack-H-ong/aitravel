package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.ErrorMessage;
import com.aitravel.common.PageResult;
import com.aitravel.dto.LoginRequest;
import com.aitravel.dto.RegisterRequest;
import com.aitravel.dto.UpdateUserRequest;
import com.aitravel.entity.User;
import com.aitravel.mapper.UserMapper;
import com.aitravel.security.JwtUtil;
import com.aitravel.service.impl.UserServiceImpl;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.LoginVO;
import com.aitravel.vo.UserVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link UserServiceImpl} 单元测试（详细设计 §3.7）。
 *
 * <p>Mock {@link UserMapper}；{@link PasswordEncoder} 使用真实 {@link BCryptPasswordEncoder}；
 * {@link JwtUtil} 使用真实对象（测试密钥 ≥ 32 字节）；不启动 Spring 容器。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";
    private static final long EXPIRES_IN = 604_800_000L;
    private static final String RAW_PASSWORD = "123456";

    @Mock
    private UserMapper userMapper;

    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtUtil = new JwtUtil(SECRET, EXPIRES_IN);
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtil, EXPIRES_IN);
    }

    // ---------- 注册 ----------

    @Test
    void registerEncodesPasswordAndSetsDefaults() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword(RAW_PASSWORD);
        request.setConfirmPassword(RAW_PASSWORD);

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(captor.capture());
        User saved = captor.getValue();
        assertNotEquals(RAW_PASSWORD, saved.getPassword());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, saved.getPassword()));
        assertEquals("user", saved.getRole());
        assertEquals(1, saved.getStatus().intValue());
        assertEquals("alice", saved.getNickname());
        assertEquals("alice", saved.getUsername());
    }

    @Test
    void registerPasswordMismatchThrowsAndNeverInserts() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword(RAW_PASSWORD);
        request.setConfirmPassword("654321");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.register(request));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals(ErrorMessage.PASSWORD_NOT_MATCH, ex.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerDuplicateUsernameThrowsUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword(RAW_PASSWORD);
        request.setConfirmPassword(RAW_PASSWORD);
        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("dup"));

        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.register(request));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals(ErrorMessage.USERNAME_EXISTS, ex.getMessage());
    }

    // ---------- 登录 ----------

    @Test
    void loginSuccessReturnsTokenAndUserWithoutPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));

        LoginVO result = userService.login(loginRequest("alice", RAW_PASSWORD));

        assertNotNull(result.getToken());
        assertFalse(result.getToken().isBlank());
        assertEquals(EXPIRES_IN, result.getExpiresIn());
        assertNotNull(result.getUser());
        assertEquals(1L, result.getUser().getId());
        assertEquals("alice", result.getUser().getUsername());
        assertFalse(hasPasswordField(UserVO.class), "UserVO 不应包含 password 字段");
    }

    @Test
    void loginUnknownUserAndWrongPasswordShareSameMessage() {
        when(userMapper.selectOne(any())).thenReturn(null);
        BusinessException unknown = assertThrows(BusinessException.class,
                () -> userService.login(loginRequest("ghost", RAW_PASSWORD)));

        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));
        BusinessException wrong = assertThrows(BusinessException.class,
                () -> userService.login(loginRequest("alice", "bad-password")));

        assertEquals(ErrorMessage.BAD_CREDENTIALS, unknown.getMessage());
        assertEquals(ErrorMessage.BAD_CREDENTIALS, wrong.getMessage());
        assertEquals(unknown.getMessage(), wrong.getMessage());
        assertEquals(ErrorCode.BAD_REQUEST, wrong.getErrorCode());
    }

    @Test
    void loginDisabledAccountThrows401AndNeverIssuesToken() {
        JwtUtil jwtSpy = spy(new JwtUtil(SECRET, EXPIRES_IN));
        UserServiceImpl serviceWithSpy =
                new UserServiceImpl(userMapper, passwordEncoder, jwtSpy, EXPIRES_IN);
        when(userMapper.selectOne(any())).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 0));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> serviceWithSpy.login(loginRequest("alice", RAW_PASSWORD)));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        assertEquals(ErrorMessage.ACCOUNT_DISABLED, ex.getMessage());
        verify(jwtSpy, never()).generateToken(any(), any(), any());
    }

    // ---------- 当前用户 ----------

    @Test
    void getCurrentUserReturnsUserWithoutPassword() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));

        UserVO result = userService.getCurrentUser(1L);

        assertEquals(1L, result.getId());
        assertEquals("alice", result.getUsername());
        assertEquals("Alice", result.getNickname());
        assertEquals(1, result.getStatus().intValue());
        assertFalse(hasPasswordField(UserVO.class), "UserVO 不应包含 password 字段");
    }

    @Test
    void getCurrentUserMissingThrowsUnauthorized() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.getCurrentUser(99L));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // ---------- 更新用户 ----------

    @Test
    void updateNicknameOnlyLeavesPasswordNull() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("Alice2");

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).updateById(captor.capture());
        assertNull(captor.getValue().getPassword(), "未改密码时不应回写 password");
        assertEquals("Alice2", captor.getValue().getNickname());
    }

    @Test
    void updatePasswordSuccessReplacesOldHash() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setOldPassword(RAW_PASSWORD);
        request.setNewPassword("abc123456");

        userService.updateUser(1L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).updateById(captor.capture());
        String stored = captor.getValue().getPassword();
        assertTrue(passwordEncoder.matches("abc123456", stored));
        assertFalse(passwordEncoder.matches(RAW_PASSWORD, stored));
    }

    @Test
    void updateWithoutOldPasswordThrows400AndNeverUpdates() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNewPassword("abc123456");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.updateUser(1L, request));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateWrongOldPasswordThrows400AndNeverUpdates() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));
        UpdateUserRequest request = new UpdateUserRequest();
        request.setOldPassword("wrong-old");
        request.setNewPassword("abc123456");

        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.updateUser(1L, request));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals(ErrorMessage.OLD_PASSWORD_WRONG, ex.getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateWithNoOptionalFieldsStillSucceedsAndKeepsPasswordNull() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));

        userService.updateUser(1L, new UpdateUserRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).updateById(captor.capture());
        assertNull(captor.getValue().getPassword());
    }

    @Test
    void updateUserMissingThrowsUnauthorized() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(99L, new UpdateUserRequest()));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // ---------- 后台管理 ----------

    @Test
    void pageUsersForAdminMapsRecordsWithoutPassword() {
        Page<User> page = new Page<>(1, 10);
        page.setRecords(List.of(user(1L, "alice", RAW_PASSWORD, "user", 1)));
        page.setTotal(1L);
        doReturn(page).when(userMapper).selectPage(any(), any());

        PageResult<AdminUserVO> result = userService.pageUsersForAdmin("ali", 1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPage());
        assertEquals(10L, result.getSize());
        assertFalse(hasPasswordField(AdminUserVO.class), "AdminUserVO 不应包含 password 字段");
    }

    @Test
    void updateStatusMissingUserThrowsNotFound() {
        BusinessException ex =
                assertThrows(BusinessException.class, () -> userService.updateStatus(99L, 0));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateStatusUpdatesAndPersists() {
        when(userMapper.selectById(1L)).thenReturn(user(1L, "alice", RAW_PASSWORD, "user", 1));

        userService.updateStatus(1L, 0);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus().intValue());
    }

    // ---------- helpers ----------

    private static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private User user(Long id, String username, String rawPassword, String role, int status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname("Alice");
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private static boolean hasPasswordField(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .anyMatch("password"::equals);
    }
}
