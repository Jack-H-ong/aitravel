package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 仅含主键与创建时间的实体基类。
 *
 * <p>适用于没有 {@code update_at}/{@code deleted} 列的表（{@code trip_spot}、{@code favorite}、
 * {@code ai_chat} 等）。
 */
@Data
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（数据库自增）。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;
}
