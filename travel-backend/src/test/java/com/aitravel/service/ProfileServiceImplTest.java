package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.common.PageResult;
import com.aitravel.dto.ProfileHistoryQuery;
import com.aitravel.service.impl.ProfileServiceImpl;
import com.aitravel.vo.BrowseHistoryVO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link ProfileServiceImpl} 单元测试（详细设计 §7.6）。
 *
 * <p>Mock {@link ScenicService}，不启动 Spring 容器；未登录由 M-SECURITY 拦截，本模块不测。
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final long USER_ID = 1L;

    @Mock
    private ScenicService scenicService;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(scenicService);
    }

    @Test
    void pageBrowseHistory_delegatesWithGivenArgs() {
        PageResult<BrowseHistoryVO> expected =
                new PageResult<>(List.of(), 0L, 2L, 20L);
        when(scenicService.pageBrowseHistory(USER_ID, 2, 20)).thenReturn(expected);

        PageResult<BrowseHistoryVO> actual = profileService.pageBrowseHistory(USER_ID, 2, 20);

        // 参数正确透传 + 返回值原样透传
        verify(scenicService).pageBrowseHistory(USER_ID, 2, 20);
        assertSame(expected, actual);
    }

    @Test
    void pageBrowseHistory_mapsFieldsIncludingImagesList() {
        BrowseHistoryVO vo = new BrowseHistoryVO();
        vo.setScenicId(2001L);
        vo.setName("西湖");
        vo.setCity("杭州");
        vo.setCategory("自然");
        vo.setImages(List.of("https://cdn.example.com/scenic/xihu-1.jpg",
                "https://cdn.example.com/scenic/xihu-2.jpg"));
        vo.setAvgRating(new BigDecimal("4.80"));
        vo.setBrowseTime(LocalDateTime.of(2026, 10, 12, 9, 30));
        PageResult<BrowseHistoryVO> mock =
                new PageResult<>(List.of(vo), 1L, 1L, 10L);
        when(scenicService.pageBrowseHistory(USER_ID, 1, 10)).thenReturn(mock);

        PageResult<BrowseHistoryVO> result = profileService.pageBrowseHistory(USER_ID, 1, 10);

        assertEquals(1, result.getRecords().size());
        BrowseHistoryVO actual = result.getRecords().get(0);
        assertEquals(2001L, actual.getScenicId());
        assertEquals("西湖", actual.getName());
        assertEquals("杭州", actual.getCity());
        assertEquals("自然", actual.getCategory());
        assertEquals(2, actual.getImages().size());
        assertEquals("https://cdn.example.com/scenic/xihu-1.jpg", actual.getImages().get(0));
        assertEquals(new BigDecimal("4.80"), actual.getAvgRating());
        assertEquals(LocalDateTime.of(2026, 10, 12, 9, 30), actual.getBrowseTime());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void pageBrowseHistory_noRecords_returnsEmptyNonNullRecords() {
        when(scenicService.pageBrowseHistory(USER_ID, 1, 10))
                .thenReturn(new PageResult<>(null, 0L, 1L, 10L));

        PageResult<BrowseHistoryVO> result = profileService.pageBrowseHistory(USER_ID, 1, 10);

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void sizeOver100_failsBeanValidation() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            ProfileHistoryQuery over = new ProfileHistoryQuery();
            over.setSize(101);
            Set<?> violations = validator.validate(over);
            assertFalse(violations.isEmpty(), "size=101 应产生约束违例");

            ProfileHistoryQuery valid = new ProfileHistoryQuery();
            assertTrue(validator.validate(valid).isEmpty(), "默认 page/size 应合法");

            ProfileHistoryQuery pageZero = new ProfileHistoryQuery();
            pageZero.setPage(0);
            assertFalse(validator.validate(pageZero).isEmpty(), "page=0 应产生约束违例");
        }
    }
}
