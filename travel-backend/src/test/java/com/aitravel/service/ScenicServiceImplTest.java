package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.ErrorMessage;
import com.aitravel.common.PageResult;
import com.aitravel.dto.ScenicQuery;
import com.aitravel.dto.ScenicSaveRequest;
import com.aitravel.entity.BrowseHistory;
import com.aitravel.entity.Favorite;
import com.aitravel.entity.ScenicSpot;
import com.aitravel.mapper.BrowseHistoryMapper;
import com.aitravel.mapper.FavoriteMapper;
import com.aitravel.mapper.ScenicSpotMapper;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.service.impl.ScenicServiceImpl;
import com.aitravel.vo.BrowseHistoryVO;
import com.aitravel.vo.FavoriteVO;
import com.aitravel.vo.ScenicDetailVO;
import com.aitravel.vo.ScenicVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * {@link ScenicServiceImpl} 单元测试（详细设计 §4.7）。
 *
 * <p>Mock {@link ScenicSpotMapper}、{@link FavoriteMapper}、{@link BrowseHistoryMapper}；
 * 不启动 Spring 容器，无跨模块依赖。
 */
@ExtendWith(MockitoExtension.class)
class ScenicServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long SCENIC_ID = 2001L;

    @Mock
    private ScenicSpotMapper scenicSpotMapper;

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private BrowseHistoryMapper browseHistoryMapper;

    private ScenicServiceImpl scenicService;

    @BeforeEach
    void setUp() {
        scenicService = new ScenicServiceImpl(scenicSpotMapper, favoriteMapper,
                browseHistoryMapper);
    }

    // ---------- 列表 ----------

    @Test
    @SuppressWarnings("unchecked")
    void pageScenicBuildsConditionsAndSplitsTagsImages() {
        ScenicQuery query = new ScenicQuery();
        query.setCity("杭州");
        query.setCategory("自然");
        query.setKeyword("湖");
        query.setPage(1);
        query.setSize(10);
        ScenicSpot spot = spot(SCENIC_ID);
        doReturn(pageOf(spot, 1L)).when(scenicSpotMapper).selectPage(any(), any());
        when(favoriteMapper.selectList(any()))
                .thenReturn(List.of(favorite(9L, USER_ID, SCENIC_ID)));

        PageResult<ScenicVO> result = scenicService.pageScenic(query, USER_ID);

        ArgumentCaptor<LambdaQueryWrapper<ScenicSpot>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(scenicSpotMapper).selectPage(any(), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("city"), "应包含 city 条件");
        assertTrue(sql.contains("category"), "应包含 category 条件");
        assertTrue(sql.contains("name"), "关键字应匹配 name");
        assertTrue(sql.contains("description"), "关键字应匹配 description");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("杭州"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("自然"));

        assertEquals(1, result.getRecords().size());
        ScenicVO vo = result.getRecords().get(0);
        assertEquals(List.of("自然", "免费", "亲子"), vo.getTags());
        assertEquals(List.of("u1", "u2"), vo.getImages());
        assertEquals(0, new BigDecimal("4.80").compareTo(vo.getAvgRating()));
        assertEquals(120, vo.getFavoriteCount().intValue());
        assertEquals(3400, vo.getViewCount().intValue());
        assertTrue(vo.getIsFavorite());
    }

    @Test
    void pageScenicMapsPagingFields() {
        ScenicQuery query = new ScenicQuery();
        ScenicSpot spot = spot(SCENIC_ID);
        spot.setTags(null);
        spot.setImages("");
        doReturn(pageOf(spot, 1L)).when(scenicSpotMapper).selectPage(any(), any());
        when(favoriteMapper.selectList(any())).thenReturn(List.of());

        PageResult<ScenicVO> result = scenicService.pageScenic(query, USER_ID);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPage());
        assertEquals(10L, result.getSize());
        ScenicVO vo = result.getRecords().get(0);
        assertEquals(List.of(), vo.getTags());
        assertEquals(List.of(), vo.getImages());
        assertFalse(vo.getIsFavorite());
    }

    @Test
    void pageScenicNotLoggedInNeverQueriesFavorites() {
        ScenicQuery query = new ScenicQuery();
        doReturn(pageOf(spot(SCENIC_ID), 1L)).when(scenicSpotMapper).selectPage(any(), any());

        PageResult<ScenicVO> result = scenicService.pageScenic(query, null);

        assertFalse(result.getRecords().get(0).getIsFavorite());
        verify(favoriteMapper, never()).selectList(any());
    }

    @Test
    void pageScenicEmptyResultReturnsNonNullEmptyRecords() {
        ScenicQuery query = new ScenicQuery();
        Page<ScenicSpot> empty = new Page<>(1, 10);
        empty.setTotal(0L);
        doReturn(empty).when(scenicSpotMapper).selectPage(any(), any());

        PageResult<ScenicVO> result = scenicService.pageScenic(query, null);

        assertNotNull(result.getRecords());
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getPage());
    }

    // ---------- 详情 ----------

    @Test
    void detailLoggedInIncrementsViewUpsertsAndReadsFavorite() {
        ScenicSpot spot = spot(SCENIC_ID);
        spot.setViewCount(100);
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot);
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        ScenicDetailVO vo = scenicService.detail(SCENIC_ID, USER_ID);

        assertEquals(101, vo.getViewCount().intValue());
        assertTrue(vo.getIsFavorite());
        assertEquals(List.of("自然", "免费", "亲子"), vo.getTags());
        verify(scenicSpotMapper, times(1)).incrementViewCount(SCENIC_ID);
        verify(browseHistoryMapper, times(1)).upsert(USER_ID, SCENIC_ID);
    }

    @Test
    void detailNotLoggedInSkipsUpsertAndFavoriteButStillIncrementsView() {
        ScenicSpot spot = spot(SCENIC_ID);
        spot.setViewCount(5);
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot);

        ScenicDetailVO vo = scenicService.detail(SCENIC_ID, null);

        assertFalse(vo.getIsFavorite());
        assertEquals(6, vo.getViewCount().intValue());
        verify(scenicSpotMapper, times(1)).incrementViewCount(SCENIC_ID);
        verify(browseHistoryMapper, never()).upsert(any(), any());
        verify(favoriteMapper, never()).selectCount(any());
    }

    @Test
    void detailLoggedInNotFavoriteReturnsFalse() {
        ScenicSpot spot = spot(SCENIC_ID);
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot);
        when(favoriteMapper.selectCount(any())).thenReturn(0L);

        ScenicDetailVO vo = scenicService.detail(SCENIC_ID, USER_ID);

        assertFalse(vo.getIsFavorite());
        verify(browseHistoryMapper, times(1)).upsert(USER_ID, SCENIC_ID);
    }

    @Test
    void detailNotFoundThrows404AndNeverIncrementsView() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> scenicService.detail(SCENIC_ID, USER_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertEquals(ErrorMessage.SCENIC_NOT_FOUND, ex.getMessage());
        verify(scenicSpotMapper, never()).incrementViewCount(anyLong());
        verify(browseHistoryMapper, never()).upsert(any(), any());
    }

    // ---------- 收藏 ----------

    @Test
    void toggleFavoriteWhenAbsentInsertsAndIncrementsCount() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot(SCENIC_ID));
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(favoriteMapper.insert(any(Favorite.class))).thenReturn(1);

        FavoriteVO vo = scenicService.toggleFavorite(USER_ID, SCENIC_ID);

        assertEquals("favorite", vo.getAction());
        assertTrue(vo.getIsFavorite());
        verify(favoriteMapper, times(1)).insert(any(Favorite.class));
        verify(scenicSpotMapper, times(1)).incrementFavoriteCount(SCENIC_ID);
        verify(scenicSpotMapper, never()).decrementFavoriteCount(anyLong());
    }

    @Test
    void toggleFavoriteWhenPresentDeletesAndDecrementsCount() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot(SCENIC_ID));
        when(favoriteMapper.selectOne(any())).thenReturn(favorite(9L, USER_ID, SCENIC_ID));

        FavoriteVO vo = scenicService.toggleFavorite(USER_ID, SCENIC_ID);

        assertEquals("unfavorite", vo.getAction());
        assertFalse(vo.getIsFavorite());
        verify(favoriteMapper, times(1)).deleteById(9L);
        verify(scenicSpotMapper, times(1)).decrementFavoriteCount(SCENIC_ID);
        verify(favoriteMapper, never()).insert(any(Favorite.class));
    }

    @Test
    void toggleFavoriteNotFoundThrows404AndNeverWrites() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> scenicService.toggleFavorite(USER_ID, SCENIC_ID));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(favoriteMapper, never()).insert(any(Favorite.class));
        verify(favoriteMapper, never()).deleteById(anyLong());
        verify(scenicSpotMapper, never()).incrementFavoriteCount(anyLong());
        verify(scenicSpotMapper, never()).decrementFavoriteCount(anyLong());
    }

    @Test
    void toggleFavoriteConcurrentDuplicateReturnsFavoriteWithoutIncrement() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot(SCENIC_ID));
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(favoriteMapper.insert(any(Favorite.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        FavoriteVO vo = scenicService.toggleFavorite(USER_ID, SCENIC_ID);

        assertEquals("favorite", vo.getAction());
        assertTrue(vo.getIsFavorite());
        verify(scenicSpotMapper, never()).incrementFavoriteCount(anyLong());
    }

    // ---------- 我的收藏 ----------

    @Test
    void myFavoritesMapsRecordsWithFavoriteTrue() {
        doReturn(pageOf(spot(SCENIC_ID), 1L)).when(favoriteMapper)
                .selectFavoriteScenicPage(any(), eq(USER_ID));

        PageResult<ScenicVO> result = scenicService.myFavorites(USER_ID, 1, 10);

        assertEquals(1, result.getRecords().size());
        assertTrue(result.getRecords().get(0).getIsFavorite());
        assertEquals(1L, result.getTotal());
    }

    // ---------- 城市简要信息 ----------

    @Test
    void listByCityMapsToBrief() {
        when(scenicSpotMapper.selectList(any())).thenReturn(List.of(spot(SCENIC_ID)));

        List<ScenicBrief> briefs = scenicService.listByCity("杭州");

        assertEquals(1, briefs.size());
        ScenicBrief brief = briefs.get(0);
        assertEquals(SCENIC_ID, brief.id());
        assertEquals("西湖", brief.name());
        assertEquals("杭州", brief.city());
        assertEquals("自然", brief.category());
        assertEquals("自然,免费,亲子", brief.tags());
        assertEquals(0, new BigDecimal("0.00").compareTo(brief.price()));
    }

    @Test
    void listBriefByCityDelegatesToListByCity() {
        when(scenicSpotMapper.selectList(any())).thenReturn(List.of(spot(SCENIC_ID)));

        assertEquals(1, scenicService.listBriefByCity("杭州").size());
    }

    // ---------- 浏览历史 ----------

    @Test
    void pageBrowseHistoryJoinsSpotInfoAndSplitsImages() {
        LocalDateTime time = LocalDateTime.now();
        BrowseHistory history = new BrowseHistory();
        history.setScenicId(SCENIC_ID);
        history.setBrowseTime(time);
        Page<BrowseHistory> page = new Page<>(1, 10);
        page.setRecords(List.of(history));
        page.setTotal(1L);
        doReturn(page).when(browseHistoryMapper).selectPage(any(), any());
        when(scenicSpotMapper.selectBatchIds(any())).thenReturn(List.of(spot(SCENIC_ID)));

        PageResult<BrowseHistoryVO> result = scenicService.pageBrowseHistory(USER_ID, 1, 10);

        assertEquals(1, result.getRecords().size());
        BrowseHistoryVO vo = result.getRecords().get(0);
        assertEquals(SCENIC_ID, vo.getScenicId());
        assertEquals("西湖", vo.getName());
        assertEquals(List.of("u1", "u2"), vo.getImages());
        assertEquals(time, vo.getBrowseTime());
    }

    @Test
    void pageBrowseHistoryEmptyDoesNotQuerySpots() {
        Page<BrowseHistory> page = new Page<>(1, 10);
        page.setTotal(0L);
        doReturn(page).when(browseHistoryMapper).selectPage(any(), any());

        PageResult<BrowseHistoryVO> result = scenicService.pageBrowseHistory(USER_ID, 1, 10);

        assertTrue(result.getRecords().isEmpty());
        verify(scenicSpotMapper, never()).selectBatchIds(any());
    }

    // ---------- 后台 CRUD ----------

    @Test
    void createSpotJoinsTagsAndImages() {
        ScenicSaveRequest req = new ScenicSaveRequest();
        req.setName("宋城");
        req.setCity("杭州");
        req.setTags(List.of("人文", "演出"));
        req.setImages(List.of("a.jpg", "b.jpg"));
        when(scenicSpotMapper.insert(any(ScenicSpot.class))).thenAnswer(invocation -> {
            ScenicSpot saved = invocation.getArgument(0);
            saved.setId(3001L);
            return 1;
        });

        Long id = scenicService.createSpot(req);

        assertEquals(3001L, id);
        ArgumentCaptor<ScenicSpot> captor = ArgumentCaptor.forClass(ScenicSpot.class);
        verify(scenicSpotMapper).insert(captor.capture());
        assertEquals("人文,演出", captor.getValue().getTags());
        assertEquals("a.jpg,b.jpg", captor.getValue().getImages());
        assertEquals("宋城", captor.getValue().getName());
    }

    @Test
    void updateSpotMissingThrows404AndNeverUpdates() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> scenicService.updateSpot(SCENIC_ID, new ScenicSaveRequest()));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(scenicSpotMapper, never()).updateById(any(ScenicSpot.class));
    }

    @Test
    void updateSpotMergesRequestFields() {
        when(scenicSpotMapper.selectById(SCENIC_ID)).thenReturn(spot(SCENIC_ID));
        ScenicSaveRequest req = new ScenicSaveRequest();
        req.setName("新西湖");
        req.setCity("杭州");
        req.setTags(List.of("免费"));

        scenicService.updateSpot(SCENIC_ID, req);

        ArgumentCaptor<ScenicSpot> captor = ArgumentCaptor.forClass(ScenicSpot.class);
        verify(scenicSpotMapper).updateById(captor.capture());
        assertEquals("新西湖", captor.getValue().getName());
        assertEquals("免费", captor.getValue().getTags());
        assertEquals(SCENIC_ID, captor.getValue().getId());
    }

    @Test
    void deleteSpotDelegatesToLogicDelete() {
        scenicService.deleteSpot(SCENIC_ID);

        verify(scenicSpotMapper, times(1)).deleteById(SCENIC_ID);
    }

    // ---------- helpers ----------

    private static Page<ScenicSpot> pageOf(ScenicSpot spot, long total) {
        Page<ScenicSpot> page = new Page<>(1, 10);
        page.setRecords(List.of(spot));
        page.setTotal(total);
        return page;
    }

    private static Favorite favorite(Long id, Long userId, Long scenicId) {
        Favorite favorite = new Favorite();
        favorite.setId(id);
        favorite.setUserId(userId);
        favorite.setScenicId(scenicId);
        return favorite;
    }

    private static ScenicSpot spot(Long id) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName("西湖");
        spot.setCity("杭州");
        spot.setAddress("杭州市西湖区");
        spot.setCategory("自然");
        spot.setPrice(new BigDecimal("0.00"));
        spot.setOpenTime("全天");
        spot.setDescription("杭州著名景点");
        spot.setTags("自然,免费,亲子");
        spot.setImages("u1,u2");
        spot.setAvgRating(new BigDecimal("4.80"));
        spot.setFavoriteCount(120);
        spot.setViewCount(3400);
        return spot;
    }
}
