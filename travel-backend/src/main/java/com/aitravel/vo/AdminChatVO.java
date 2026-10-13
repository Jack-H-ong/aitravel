package com.aitravel.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 后台对话记录视图对象（详细设计 §6.2，供 M-ADMIN 对话记录管理）。
 *
 * <p>由 {@code AiChatMapper} 自定义 JOIN 查询直接映射：{@code user_id}/{@code create_at} 经
 * {@code map-underscore-to-camel-case} 映射为驼峰字段。
 */
@Data
public class AdminChatVO {

    /** 对话主键。 */
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 归属用户名。 */
    private String username;

    /** 关联行程 ID，可为 {@code null}。 */
    private Long tripId;

    /** 角色：user / assistant。 */
    private String role;

    /** 对话内容。 */
    private String content;

    /** 创建时间。 */
    private LocalDateTime createAt;
}
