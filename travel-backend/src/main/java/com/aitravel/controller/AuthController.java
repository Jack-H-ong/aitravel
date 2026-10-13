package com.aitravel.controller;

import com.aitravel.common.Result;
import com.aitravel.dto.LoginRequest;
import com.aitravel.dto.RegisterRequest;
import com.aitravel.dto.UpdateUserRequest;
import com.aitravel.security.UserContext;
import com.aitravel.security.annotation.PublicApi;
import com.aitravel.service.UserService;
import com.aitravel.vo.LoginVO;
import com.aitravel.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户与认证接口（详细设计 §3.3）。
 *
 * <p>{@code /register}、{@code /login} 标注 {@link PublicApi} 无需登录；
 * {@code /me}、{@code /update} 需登录，{@code userId} 取自 {@link UserContext}。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    /**
     * 构造控制器。
     *
     * @param userService 用户服务
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册（§3.3.1）。
     *
     * @param request 注册请求
     * @return 空数据成功响应
     */
    @PostMapping("/register")
    @PublicApi
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.ok();
    }

    /**
     * 用户登录（§3.3.2）。
     *
     * @param request 登录请求
     * @return 令牌、有效期与用户信息
     */
    @PostMapping("/login")
    @PublicApi
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    /**
     * 当前用户信息（§3.3.3，需登录）。
     *
     * @return 当前用户视图对象（不含密码）
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getCurrentUser(UserContext.getUserId()));
    }

    /**
     * 更新用户信息（§3.3.4，需登录）。
     *
     * @param request 更新请求
     * @return 空数据成功响应
     */
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody UpdateUserRequest request) {
        userService.updateUser(UserContext.getUserId(), request);
        return Result.ok();
    }
}
