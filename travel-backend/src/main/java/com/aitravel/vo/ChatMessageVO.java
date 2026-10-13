package com.aitravel.vo;

import com.aitravel.entity.AiChat;
import java.time.LocalDateTime;
import lombok.Data;

/** 单条对话历史视图对象（详细设计 §6.3.2）。 */
@Data
public class ChatMessageVO {

    /** 对话主键。 */
    private Long id;

    /** 关联行程 ID，可为 {@code null}。 */
    private Long tripId;

    /** 角色：user / assistant。 */
    private String role;

    /** 对话内容。 */
    private String content;

    /** 创建时间。 */
    private LocalDateTime createAt;

    /**
     * 由实体构造历史视图对象。
     *
     * @param chat 对话实体，可为 {@code null}
     * @return 历史视图对象；入参为 {@code null} 时返回 {@code null}
     */
    public static ChatMessageVO from(AiChat chat) {
        if (chat == null) {
            return null;
        }
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(chat.getId());
        vo.setTripId(chat.getTripId());
        vo.setRole(chat.getRole());
        vo.setContent(chat.getContent());
        vo.setCreateAt(chat.getCreateAt());
        return vo;
    }
}
