package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 浏览记录表 {@code browse_history} 实体。
 *
 * <p>该表没有 {@code create_at} 列，只有 {@code browse_time}，故 <b>不继承</b> {@link BaseEntity}，
 * 独立声明字段（{@code createAt} 自动填充对其不适用，{@code browse_time} 由 SQL 维护）。
 */
@Data
@TableName("browse_history")
public class BrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（数据库自增）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 ID。 */
    private Long userId;

    /** 浏览的景点 ID。 */
    private Long scenicId;

    /** 浏览时间。 */
    private LocalDateTime browseTime;
}
