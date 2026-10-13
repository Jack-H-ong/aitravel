package com.aitravel.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aitravel.entity.ScenicSpot;
import com.aitravel.entity.TripDay;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * M-PERSIST 集成测试（H2 + {@code MODE=MySQL}），覆盖 §2.2.4 的用例。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class PersistenceIntegrationTest {

    @Autowired
    private ScenicSpotMapper scenicSpotMapper;

    @Autowired
    private TripDayMapper tripDayMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ScenicSpot newScenic() {
        ScenicSpot spot = new ScenicSpot();
        spot.setName("集成测试景点");
        spot.setCity("集成测试城市");
        spot.setCategory("自然");
        spot.setPrice(new BigDecimal("12.50"));
        spot.setFavoriteCount(0);
        spot.setViewCount(0);
        return spot;
    }

    @Test
    void insertFillsCreateAtAndUpdateAt() {
        ScenicSpot spot = newScenic();
        scenicSpotMapper.insert(spot);

        assertThat(spot.getId()).isNotNull();
        ScenicSpot loaded = scenicSpotMapper.selectById(spot.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getCreateAt()).isNotNull();
        assertThat(loaded.getUpdateAt()).isNotNull();
        assertThat(loaded.getDeleted()).isEqualTo(0);
    }

    @Test
    void updateRefreshesUpdateAt() throws InterruptedException {
        ScenicSpot spot = newScenic();
        scenicSpotMapper.insert(spot);
        LocalDateTime before = scenicSpotMapper.selectById(spot.getId()).getUpdateAt();

        Thread.sleep(1100L);

        ScenicSpot patch = new ScenicSpot();
        patch.setId(spot.getId());
        patch.setName("集成测试景点-已更新");
        scenicSpotMapper.updateById(patch);

        ScenicSpot after = scenicSpotMapper.selectById(spot.getId());
        assertThat(after.getName()).isEqualTo("集成测试景点-已更新");
        assertThat(after.getUpdateAt()).isAfter(before);
    }

    @Test
    void pageWithNoMatchingRowsReturnsEmptyNonNullRecords() {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicSpot::getCity, "分页测试-不存在的城市");

        Page<ScenicSpot> result = scenicSpotMapper.selectPage(new Page<>(1, 10), wrapper);

        assertThat(result.getRecords()).isNotNull();
        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotal()).isZero();
    }

    @Test
    void logicalDeleteHidesRowButKeepsItInTable() {
        ScenicSpot spot = newScenic();
        scenicSpotMapper.insert(spot);
        Long id = spot.getId();

        scenicSpotMapper.deleteById(id);

        assertThat(scenicSpotMapper.selectById(id)).isNull();
        Integer deleted = jdbcTemplate.queryForObject(
                "SELECT deleted FROM scenic_spot WHERE id = ?", Integer.class, id);
        assertThat(deleted).isEqualTo(1);
    }

    @Test
    void updateWithoutWhereIsBlockedByBlockAttack() {
        // 使用无 @TableLogic 的实体（trip_day），空 wrapper 不会自动追加 deleted 条件，
        // 生成真正的无 where 全表 update，触发 BlockAttackInnerInterceptor。
        LambdaUpdateWrapper<TripDay> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(TripDay::getTheme, "no-where-update");

        // MyBatis-Spring 会把拦截器抛出的 MybatisPlusException 包装为 MyBatisSystemException，
        // 故断言最根本的 cause 为 MybatisPlusException。
        assertThatThrownBy(() -> tripDayMapper.update(null, wrapper))
                .hasRootCauseInstanceOf(MybatisPlusException.class);
    }

    @Test
    void incrementViewCountIsAtomicOnH2() {
        ScenicSpot spot = newScenic();
        spot.setViewCount(5);
        scenicSpotMapper.insert(spot);

        int affected = scenicSpotMapper.incrementViewCount(spot.getId());

        assertThat(affected).isEqualTo(1);
        assertThat(scenicSpotMapper.selectById(spot.getId()).getViewCount()).isEqualTo(6);
    }
}
