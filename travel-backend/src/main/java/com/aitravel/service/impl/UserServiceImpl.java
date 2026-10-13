package com.aitravel.service.impl;

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
import com.aitravel.service.UserService;
import com.aitravel.vo.AdminUserVO;
import com.aitravel.vo.LoginVO;
import com.aitravel.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户与认证服务实现（详细设计 §3.5）。
 *
 * <p>注册唯一性仅依赖 {@code uk_username} 唯一键兜底（不做「先查后插」，见 §3.5.1）；
 * 登录用户名/密码错误统一为同一文案，防止用户名枚举（§3.5.2）。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 UserMapper/PasswordEncoder/JwtUtil 由 Spring 容器持有，此处仅保存引用")
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long jwtExpiration;

    /**
     * 构造服务实现。
     *
     * @param userMapper     用户数据访问
     * @param passwordEncoder 密码编码器（BCrypt）
     * @param jwtUtil        JWT 工具
     * @param jwtExpiration  Token 有效期毫秒（{@code jwt.expiration}），用于登录响应 {@code expiresIn}
     */
    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           @Value("${jwt.expiration}") long jwtExpiration) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtExpiration = jwtExpiration;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorMessage.PASSWORD_NOT_MATCH);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole("user");
        user.setStatus(1);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 由 uk_username 兜底：并发/重复注册统一收敛为同一错误
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorMessage.USERNAME_EXISTS);
        }
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 同一文案，防用户名枚举
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorMessage.BAD_CREDENTIALS);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, ErrorMessage.ACCOUNT_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, jwtExpiration, UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return UserVO.from(user);
    }

    @Override
    @Transactional
    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (StringUtils.hasText(request.getNewPassword())) {
            if (!StringUtils.hasText(request.getOldPassword())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorMessage.OLD_PASSWORD_WRONG);
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        } else {
            // 未改密码：置空以令动态 SQL 跳过 password 列，避免把库中原哈希回写
            user.setPassword(null);
        }
        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        userMapper.updateById(user);
    }

    @Override
    public PageResult<AdminUserVO> pageUsersForAdmin(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        IPage<AdminUserVO> converted = result.convert(AdminUserVO::from);
        return PageResult.of(converted, AdminUserVO.class);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }
}
