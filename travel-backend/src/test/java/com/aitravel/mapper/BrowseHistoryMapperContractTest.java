package com.aitravel.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

/**
 * Mapper 契约单测：断言自定义注解 SQL 文本。
 *
 * <p>{@code browse_history} 的 UPSERT 使用 MySQL 专有语法（{@code ON DUPLICATE KEY UPDATE}），
 * H2 不支持，故不进入 H2 集成测试，改由本测试锁定其 SQL 契约。
 */
class BrowseHistoryMapperContractTest {

    @Test
    void upsertUsesMysqlOnDuplicateKeyUpdate() throws NoSuchMethodException {
        Method method = BrowseHistoryMapper.class.getMethod("upsert", Long.class, Long.class);
        Insert insert = method.getAnnotation(Insert.class);

        assertThat(insert).isNotNull();
        String sql = String.join(" ", insert.value());
        assertThat(sql).contains("INSERT INTO browse_history");
        assertThat(sql).contains("ON DUPLICATE KEY UPDATE");
        assertThat(sql).contains("browse_time = NOW()");
    }

    @Test
    void selectRecentFiltersByTripAndOrdersByCreateAt() throws NoSuchMethodException {
        Method method = AiChatMapper.class.getMethod(
                "selectRecent", Long.class, Long.class, int.class);
        Select select = method.getAnnotation(Select.class);

        assertThat(select).isNotNull();
        String sql = String.join(" ", select.value());
        assertThat(sql).contains("FROM ai_chat");
        assertThat(sql).contains("trip_id = #{tripId}");
        assertThat(sql).contains("ORDER BY create_at DESC");
        assertThat(sql).contains("LIMIT #{limit}");
    }
}
