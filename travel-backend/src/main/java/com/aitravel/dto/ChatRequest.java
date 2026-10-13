package com.aitravel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 智能问答请求（详细设计 §6.3.1）。 */
@Data
public class ChatRequest {

    /** 用户提问内容，必填且不超过 500 字。 */
    @NotBlank(message = "消息不能为空")
    @Size(max = 500)
    private String message;

    /** 关联行程 ID，可选；传入时校验归属。 */
    private Long tripId;
}
