package com.aitravel.vo;

import java.time.LocalDateTime;
import lombok.Data;

/** 智能问答响应视图对象（详细设计 §6.3.1）。 */
@Data
public class ChatVO {

    /** AI 回答文本。 */
    private String answer;

    /** 回答落库时间（取自 assistant 记录的 {@code create_at}）。 */
    private LocalDateTime createdAt;

    /** Jackson 反序列化用无参构造。 */
    public ChatVO() {
    }

    /**
     * 全量构造。
     *
     * @param answer    AI 回答文本
     * @param createdAt 回答落库时间
     */
    public ChatVO(String answer, LocalDateTime createdAt) {
        this.answer = answer;
        this.createdAt = createdAt;
    }
}
