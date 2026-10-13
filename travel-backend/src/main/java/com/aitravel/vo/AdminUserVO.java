package com.aitravel.vo;

import com.aitravel.entity.User;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台用户视图对象（详细设计 §3.2，供 M-ADMIN 用户管理）。
 *
 * <p><b>刻意不包含 {@code password}</b>。{@code id} 为 {@code Long}，序列化时由
 * {@code JacksonConfig} 统一转为字符串；{@code createAt} 由全局 Jackson 配置格式化。
 */
@Data
public class AdminUserVO {

    /** 用户主键。 */
    private Long id;

    /** 用户名。 */
    private String username;

    /** 昵称。 */
    private String nickname;

    /** 头像 URL。 */
    private String avatar;

    /** 角色：user/admin。 */
    private String role;

    /** 状态：0=禁用，1=正常。 */
    private Integer status;

    /** 注册时间。 */
    private LocalDateTime createAt;

    /**
     * 由用户实体构造后台视图对象（不含 {@code password}）。
     *
     * @param user 用户实体，可为 {@code null}
     * @return 后台用户视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static AdminUserVO from(User user) {
        if (user == null) {
            return null;
        }
        AdminUserVO vo = new AdminUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateAt(user.getCreateAt());
        return vo;
    }
}
