package com.aitravel.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带审计与逻辑删除字段的实体基类。
 *
 * <p>适用于同时具备 {@code update_at} 与 {@code deleted} 列的表（{@code user}、
 * {@code scenic_spot}、{@code trip}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditableEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 更新时间，插入与更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;

    /** 逻辑删除标记：0=未删除，1=已删除。 */
    @TableLogic
    private Integer deleted;
}
