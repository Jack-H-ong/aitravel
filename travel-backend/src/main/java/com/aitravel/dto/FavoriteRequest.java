package com.aitravel.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 收藏切换请求（详细设计 §4.3.3）。 */
@Data
public class FavoriteRequest {

    /** 目标景点 ID。 */
    @NotNull(message = "景点 ID 不能为空")
    private Long scenicId;
}
