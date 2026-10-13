package com.aitravel.vo;

import com.aitravel.entity.User;
import lombok.Data;

/**
 * 用户视图对象（详细设计 §3.3.2 / §3.3.3）。
 *
 * <p><b>刻意不包含 {@code password}</b>，对外只暴露安全字段。{@code id} 为 {@code Long}，
 * 序列化时由 {@code JacksonConfig} 统一转为字符串。
 */
@Data
public class UserVO {

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

    /**
     * 由用户实体构造视图对象，仅映射安全字段（不含 {@code password}）。
     *
     * @param user 用户实体，可为 {@code null}
     * @return 用户视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static UserVO from(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        return vo;
    }
}
