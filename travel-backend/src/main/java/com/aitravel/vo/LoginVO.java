package com.aitravel.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应视图对象（详细设计 §3.3.2）。
 *
 * <p>{@code expiresIn} 为 Token 有效期（毫秒），取自 {@code jwt.expiration}。
 */
@Data
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "LoginVO 为响应 DTO，由业务层即时构造并经 Jackson 序列化返回，user 引用不对外长期持有")
public class LoginVO {

    /** JWT 令牌。 */
    private String token;

    /** 有效期（毫秒）。 */
    private long expiresIn;

    /** 当前用户信息（不含密码）。 */
    private UserVO user;
}
