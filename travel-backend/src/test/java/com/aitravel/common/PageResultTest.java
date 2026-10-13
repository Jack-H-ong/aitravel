package com.aitravel.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link PageResult} 单元测试（§2.1.4）。 */
class PageResultTest {

    @Test
    void ofMapsFieldsCorrectly() {
        Page<String> source = new Page<>(2, 5);
        source.setRecords(List.of("a", "b"));
        source.setTotal(42);

        PageResult<String> result = PageResult.of(source, String.class);

        assertEquals(List.of("a", "b"), result.getRecords());
        assertEquals(42L, result.getTotal());
        assertEquals(2L, result.getPage());
        assertEquals(5L, result.getSize());
    }

    @Test
    void emptyRecordsIsNotNull() {
        Page<String> source = new Page<>(1, 10);
        source.setRecords(null);
        source.setTotal(0);

        PageResult<String> result = PageResult.of(source, String.class);

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getPage());
        assertEquals(10L, result.getSize());
    }
}
