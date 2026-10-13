package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI 对话记录表 {@code ai_chat} 实体（仅 {@code create_at}）。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat")
public class AiChat extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 归属用户 ID。 */
    private Long userId;

    /** 关联行程 ID（可选）。 */
    private Long tripId;

    /** 角色：user/assistant。 */
    private String role;

    /** 对话内容。 */
    private String content;

    /**
     * 构造用户提问记录。
     *
     * @param userId  归属用户 ID
     * @param tripId  关联行程 ID，可为 {@code null}
     * @param content 提问内容
     * @return 角色为 {@code user} 的对话实体
     */
    public static AiChat user(Long userId, Long tripId, String content) {
        return of(userId, tripId, "user", content);
    }

    /**
     * 构造 AI 回答记录。
     *
     * @param userId  归属用户 ID
     * @param tripId  关联行程 ID，可为 {@code null}
     * @param content 回答内容
     * @return 角色为 {@code assistant} 的对话实体
     */
    public static AiChat assistant(Long userId, Long tripId, String content) {
        return of(userId, tripId, "assistant", content);
    }

    /**
     * 通用工厂。
     *
     * @param userId  归属用户 ID
     * @param tripId  关联行程 ID，可为 {@code null}
     * @param role    角色标识
     * @param content 对话内容
     * @return 对话实体
     */
    private static AiChat of(Long userId, Long tripId, String role, String content) {
        AiChat chat = new AiChat();
        chat.setUserId(userId);
        chat.setTripId(tripId);
        chat.setRole(role);
        chat.setContent(content);
        return chat;
    }
}
