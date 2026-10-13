package com.aitravel.service;

import com.aitravel.common.PageResult;
import com.aitravel.dto.LoginRequest;
import com.aitravel.dto.RegisterRequest;
import com.aitravel.dto.UpdateUserRequest;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.LoginVO;
import com.aitravel.vo.UserVO;

/**
 * 用户与认证服务（详细设计 §3.1）。
 *
 * <p>对外提供注册、登录、当前用户信息、用户信息更新，以及供 M-ADMIN 使用的用户管理能力。
 *
 * @see com.aitravel.service.impl.UserServiceImpl
 */
public interface UserService {

    /**
     * 注册新用户。
     *
     * @param request 注册请求
     * @throws com.aitravel.common.BusinessException 两次密码不一致（400）或用户名已注册（400）
     */
    void register(RegisterRequest request);

    /**
     * 登录并签发 Token。
     *
     * @param request 登录请求
     * @return 令牌、有效期与当前用户信息
     * @throws com.aitravel.common.BusinessException 用户名或密码错误（400）或账号已被禁用（401）
     */
    LoginVO login(LoginRequest request);

    /**
     * 查询当前登录用户信息。
     *
     * @param userId 当前用户主键
     * @return 用户视图对象（不含密码）
     * @throws com.aitravel.common.BusinessException 用户不存在（401）
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 更新当前用户信息（昵称/头像，可选改密码）。
     *
     * @param userId  当前用户主键
     * @param request 更新请求
     * @throws com.aitravel.common.BusinessException 用户不存在（401）、缺原密码（400）或原密码错误（400）
     */
    void updateUser(Long userId, UpdateUserRequest request);

    /**
     * 分页查询用户（供 M-ADMIN）。
     *
     * @param keyword 用户名/昵称模糊关键字，可为空
     * @param page    页码，从 1 开始
     * @param size    每页大小
     * @return 分页结果（不含密码）
     */
    PageResult<AdminUserVO> pageUsersForAdmin(String keyword, int page, int size);

    /**
     * 启用/禁用用户（供 M-ADMIN）。
     *
     * @param userId 目标用户主键
     * @param status 状态：0=禁用，1=正常
     * @throws com.aitravel.common.BusinessException 用户不存在（404）
     */
    void updateStatus(Long userId, Integer status);
}
