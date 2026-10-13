package com.aitravel.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 实体字段自动填充处理器。
 *
 * <p>与数据库默认值构成「双保险」：插入时填充 {@code createAt}/{@code updateAt}，更新时刷新
 * {@code updateAt}，使 Java 侧能直接拿到回写值。{@code strictInsertFill}/{@code strictUpdateFill}
 * 仅在实体确实声明了对应字段（且填充类型匹配）时才生效，因此在缺少这些字段的实体
 * （如 {@code trip_spot}、{@code favorite}、{@code browse_history}）上自动跳过，不会报错。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateAt", LocalDateTime.class, LocalDateTime.now());
    }
}
