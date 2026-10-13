package com.aitravel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aitravel.common.BusinessException;
import com.aitravel.common.ErrorCode;
import com.aitravel.common.ErrorMessage;
import com.aitravel.common.PageResult;
import com.aitravel.dto.GenerateTripRequest;
import com.aitravel.dto.UpdateTripRequest;
import com.aitravel.entity.Trip;
import com.aitravel.entity.TripDay;
import com.aitravel.entity.TripSpot;
import com.aitravel.mapper.TripDayMapper;
import com.aitravel.mapper.TripMapper;
import com.aitravel.mapper.TripSpotMapper;
import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiServiceException;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.service.ai.TripPlan;
import com.aitravel.service.ai.TripPlanDay;
import com.aitravel.service.ai.TripPlanSpot;
import com.aitravel.service.impl.TripPersistService;
import com.aitravel.service.impl.TripServiceImpl;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.TripDetailVO;
import com.aitravel.vo.TripVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * {@link TripServiceImpl} 单元测试（详细设计 §5.8）。
 *
 * <p>Mock {@link TripMapper}、{@link TripDayMapper}、{@link TripSpotMapper}、{@link ScenicService}、
 * {@link AiGateway}；被测对象注入真实 {@link TripPersistService}，从而在同一测试中覆盖落库次数与
 * {@code scenic_id} 回填。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TripServiceImplTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long TRIP_ID = 3001L;
    private static final long SCENIC_XIHU = 2001L;
    private static final long SCENIC_LINGYIN = 2002L;

    @Mock
    private TripMapper tripMapper;

    @Mock
    private TripDayMapper tripDayMapper;

    @Mock
    private TripSpotMapper tripSpotMapper;

    @Mock
    private ScenicService scenicService;

    @Mock
    private AiGateway aiGateway;

    private TripPersistService tripPersistService;

    private TripServiceImpl tripService;

    @BeforeEach
    void setUp() {
        tripPersistService = new TripPersistService(tripMapper, tripDayMapper, tripSpotMapper,
                scenicService);
        tripService = new TripServiceImpl(tripMapper, tripDayMapper, tripSpotMapper, scenicService,
                aiGateway, tripPersistService);
    }

    // ---------- 生成攻略：成功 ----------

    @Test
    void generateTripInsertsTripDaysSpotsAndBackfillsScenicId() {
        List<ScenicBrief> briefs = List.of(brief(SCENIC_XIHU, "西湖"), brief(SCENIC_LINGYIN, "灵隐寺"));
        TripPlan plan = new TripPlan("杭州2日休闲之旅", List.of(
                new TripPlanDay(1, "西湖漫步", List.of(
                        new TripPlanSpot("西湖", "3h", 1, 180, "可租自行车"),
                        new TripPlanSpot("灵隐寺", "2h", 2, 120, "上午人少")),
                        "楼外楼", "知味观", "早出发"),
                new TripPlanDay(2, "古镇探访", List.of(
                        new TripPlanSpot("西湖", "2h", 1, 150, "黄昏最美"),
                        new TripPlanSpot("未知古镇", "2h", 2, 90, "备选")),
                        "面馆", "小吃", "注意天气")));
        when(scenicService.listByCity("杭州")).thenReturn(briefs);
        when(aiGateway.generateTripPlan(any())).thenReturn(plan);
        stubTripInsert();
        when(tripDayMapper.insert(any(TripDay.class))).thenReturn(1);
        when(tripSpotMapper.insert(any(TripSpot.class))).thenReturn(1);

        // 生成后回读详情所需的桩
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, USER_ID));
        when(tripDayMapper.selectList(any())).thenReturn(List.of(tripDay(1, 1), tripDay(2, 2)));
        when(tripSpotMapper.selectList(any())).thenReturn(List.of(
                tripSpot(1, 1, 1, "西湖"), tripSpot(1, 2, 2, "灵隐寺"),
                tripSpot(2, 1, 1, "西湖"), tripSpot(2, 2, 2, "未知古镇")));

        GenerateTripRequest req = req("杭州", 2, List.of("美食", "自然"));
        TripDetailVO detail = tripService.generateTrip(USER_ID, req);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripMapper, times(1)).insert(tripCaptor.capture());
        Trip saved = tripCaptor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("杭州", saved.getCity());
        assertEquals("美食,自然", saved.getPreferences());
        assertEquals(1, saved.getStatus().intValue());

        verify(tripDayMapper, times(2)).insert(any(TripDay.class));

        ArgumentCaptor<TripSpot> spotCaptor = ArgumentCaptor.forClass(TripSpot.class);
        verify(tripSpotMapper, times(4)).insert(spotCaptor.capture());
        assertEquals(SCENIC_XIHU, findSpot(spotCaptor.getAllValues(), 1, "西湖").getScenicId());
        assertEquals(SCENIC_LINGYIN, findSpot(spotCaptor.getAllValues(), 1, "灵隐寺").getScenicId());
        assertNull(findSpot(spotCaptor.getAllValues(), 2, "未知古镇").getScenicId());

        assertEquals(2, detail.getDayList().size());
        verify(aiGateway, times(1)).generateTripPlan(any());
    }

    @Test
    void savePlanBackfillsScenicIdHitAndMiss() {
        when(scenicService.listByCity("杭州")).thenReturn(List.of(brief(SCENIC_XIHU, "西湖")));
        stubTripInsert();
        when(tripDayMapper.insert(any(TripDay.class))).thenReturn(1);
        when(tripSpotMapper.insert(any(TripSpot.class))).thenReturn(1);
        TripPlan plan = new TripPlan("t", List.of(new TripPlanDay(1, "主题", List.of(
                new TripPlanSpot("西湖", "3h", 1, 180, "tip"),
                new TripPlanSpot("黄龙溪", "2h", 2, 90, "tip")), "l", "d", "t")));

        Long id = tripPersistService.savePlan(USER_ID, req("杭州", 1), plan);

        assertEquals(TRIP_ID, id);
        ArgumentCaptor<TripSpot> captor = ArgumentCaptor.forClass(TripSpot.class);
        verify(tripSpotMapper, times(2)).insert(captor.capture());
        assertEquals(SCENIC_XIHU, captor.getAllValues().get(0).getScenicId());
        assertNull(captor.getAllValues().get(1).getScenicId());
    }

    // ---------- 详情装配 ----------

    @Test
    void getTripDetailSortsDaysAndSpotsAscending() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, USER_ID));
        when(tripDayMapper.selectList(any()))
                .thenReturn(new ArrayList<>(List.of(tripDay(2, 2), tripDay(1, 1))));
        when(tripSpotMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                tripSpot(1, 2, 2, "灵隐寺"), tripSpot(1, 1, 1, "西湖"), tripSpot(2, 1, 1, "乌镇"))));

        TripDetailVO detail = tripService.getTripDetail(USER_ID, TRIP_ID);

        assertEquals(List.of(1, 2),
                detail.getDayList().stream().map(d -> d.getDayNo()).toList());
        assertEquals(List.of("西湖", "灵隐寺"),
                detail.getDayList().get(0).getSpots().stream()
                        .map(s -> s.getSpotName()).toList());
        assertEquals(List.of("乌镇"),
                detail.getDayList().get(1).getSpots().stream()
                        .map(s -> s.getSpotName()).toList());
    }

    // ---------- 列表 ----------

    @Test
    @SuppressWarnings("unchecked")
    void pageTripsFiltersByUserIdAndTripVoHasNoDayList() {
        doReturn(pageOf(trip(TRIP_ID, USER_ID), 1L)).when(tripMapper).selectPage(any(), any());

        PageResult<TripVO> result = tripService.pageTrips(USER_ID, null, 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<Trip>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(tripMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("user_id"), "应按 user_id 过滤");
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue(USER_ID));

        assertEquals(1, result.getRecords().size());
        assertFalse(Arrays.stream(TripVO.class.getDeclaredFields())
                .anyMatch(f -> "dayList".equals(f.getName())), "TripVO 不应含 dayList");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageTripsWithCityAddsCityCondition() {
        doReturn(pageOf(trip(TRIP_ID, USER_ID), 1L)).when(tripMapper).selectPage(any(), any());

        tripService.pageTrips(USER_ID, "杭州", 1, 10);

        ArgumentCaptor<LambdaQueryWrapper<Trip>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(tripMapper).selectPage(any(), captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("city"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("杭州"));
    }

    // ---------- 修改 ----------

    @Test
    void updateTripUpdatesDeletesThenReinserts() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, USER_ID));
        when(tripDayMapper.delete(any())).thenReturn(1);
        when(tripSpotMapper.delete(any())).thenReturn(1);
        when(tripDayMapper.insert(any(TripDay.class))).thenReturn(1);
        when(tripSpotMapper.insert(any(TripSpot.class))).thenReturn(1);

        UpdateTripRequest.UpdateTripDay day = new UpdateTripRequest.UpdateTripDay();
        day.setDayNo(1);
        day.setTheme("西湖");
        UpdateTripRequest.UpdateTripSpot spot1 = new UpdateTripRequest.UpdateTripSpot();
        spot1.setSpotName("西湖");
        spot1.setVisitOrder(1);
        UpdateTripRequest.UpdateTripSpot spot2 = new UpdateTripRequest.UpdateTripSpot();
        spot2.setSpotName("灵隐寺");
        spot2.setVisitOrder(2);
        day.setSpots(List.of(spot1, spot2));

        UpdateTripRequest req = new UpdateTripRequest();
        req.setTitle("杭州2日·改");
        req.setDayList(List.of(day));

        tripService.updateTrip(USER_ID, TRIP_ID, req);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripMapper, times(1)).updateById(tripCaptor.capture());
        assertEquals("杭州2日·改", tripCaptor.getValue().getTitle());
        verify(tripDayMapper, times(1)).delete(any());
        verify(tripSpotMapper, times(1)).delete(any());
        verify(tripDayMapper, times(1)).insert(any(TripDay.class));
        verify(tripSpotMapper, times(2)).insert(any(TripSpot.class));
    }

    // ---------- 删除 ----------

    @Test
    void deleteTripLogicDeletesTripAndPhysicallyDeletesChildren() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, USER_ID));

        tripService.deleteTrip(USER_ID, TRIP_ID);

        verify(tripMapper, times(1)).deleteById(TRIP_ID);
        verify(tripDayMapper, times(1)).delete(any());
        verify(tripSpotMapper, times(1)).delete(any());
    }

    // ---------- 偏好为空 ----------

    @Test
    void emptyPreferencesDoesNotThrow() {
        when(scenicService.listByCity("杭州")).thenReturn(List.of(brief(SCENIC_XIHU, "西湖")));
        stubTripInsert();
        when(tripDayMapper.insert(any(TripDay.class))).thenReturn(1);
        TripPlan plan = new TripPlan("t",
                List.of(new TripPlanDay(1, "主题", List.of(), "l", "d", "t")));

        GenerateTripRequest nullPref = req("杭州", 1, null);
        tripPersistService.savePlan(USER_ID, nullPref, plan);
        GenerateTripRequest emptyPref = req("杭州", 1, List.of());
        tripPersistService.savePlan(USER_ID, emptyPref, plan);

        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripMapper, times(2)).insert(captor.capture());
        assertNull(captor.getAllValues().get(0).getPreferences());
        assertTrue(captor.getAllValues().get(1).getPreferences().isEmpty());
    }

    // ---------- 异常 ----------

    @Test
    void generateTripWithoutScenicThrows400AndNeverCallsAi() {
        when(scenicService.listByCity("无景点城")).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.generateTrip(USER_ID, req("无景点城", 1)));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertEquals(ErrorMessage.NO_SCENIC_IN_CITY, ex.getMessage());
        verify(aiGateway, never()).generateTripPlan(any());
        verify(tripMapper, never()).insert(any(Trip.class));
        verify(tripDayMapper, never()).insert(any(TripDay.class));
        verify(tripSpotMapper, never()).insert(any(TripSpot.class));
    }

    @Test
    void generateTripAiFailureConverts5001AndInsertsNothing() {
        when(scenicService.listByCity("杭州")).thenReturn(List.of(brief(SCENIC_XIHU, "西湖")));
        when(aiGateway.generateTripPlan(any())).thenThrow(new AiServiceException("boom"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.generateTrip(USER_ID, req("杭州", 1)));

        assertEquals(ErrorCode.AI_SERVICE_ERROR, ex.getErrorCode());
        assertEquals(ErrorMessage.AI_GENERATE_FAILED, ex.getMessage());
        verify(tripMapper, never()).insert(any(Trip.class));
        verify(tripDayMapper, never()).insert(any(TripDay.class));
        verify(tripSpotMapper, never()).insert(any(TripSpot.class));
    }

    @Test
    void detailOfOthersTripForbidden() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, OTHER_USER_ID));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.getTripDetail(USER_ID, TRIP_ID));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(tripDayMapper, never()).selectList(any());
        verify(tripSpotMapper, never()).selectList(any());
    }

    @Test
    void updateOfOthersTripForbiddenNeverWrites() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, OTHER_USER_ID));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.updateTrip(USER_ID, TRIP_ID, new UpdateTripRequest()));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(tripMapper, never()).updateById(any(Trip.class));
        verify(tripDayMapper, never()).delete(any());
        verify(tripSpotMapper, never()).delete(any());
        verify(tripDayMapper, never()).insert(any(TripDay.class));
        verify(tripSpotMapper, never()).insert(any(TripSpot.class));
    }

    @Test
    void deleteOfOthersTripForbiddenNeverWrites() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, OTHER_USER_ID));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tripService.deleteTrip(USER_ID, TRIP_ID));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(tripMapper, never()).deleteById(anyLong());
        verify(tripDayMapper, never()).delete(any());
        verify(tripSpotMapper, never()).delete(any());
    }

    @Test
    void missingTripThrows404ForDetailUpdateDelete() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(null);

        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class,
                () -> tripService.getTripDetail(USER_ID, TRIP_ID)).getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class,
                () -> tripService.updateTrip(USER_ID, TRIP_ID, new UpdateTripRequest()))
                .getErrorCode());
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class,
                () -> tripService.deleteTrip(USER_ID, TRIP_ID)).getErrorCode());

        verify(tripMapper, never()).updateById(any(Trip.class));
        verify(tripMapper, never()).deleteById(anyLong());
    }

    // ---------- 摘要 / 后台列表 ----------

    @Test
    void getTripSummaryContainsTitleCityDaysAndSpots() {
        when(tripMapper.selectById(TRIP_ID)).thenReturn(trip(TRIP_ID, USER_ID));
        when(tripDayMapper.selectList(any())).thenReturn(List.of(tripDay(1, 1), tripDay(2, 2)));
        when(tripSpotMapper.selectList(any())).thenReturn(List.of(
                tripSpot(1, 1, 1, "西湖"), tripSpot(1, 2, 2, "灵隐寺"), tripSpot(2, 1, 1, "乌镇")));

        String summary = tripService.getTripSummary(TRIP_ID, USER_ID);

        assertTrue(summary.contains("杭州2日休闲之旅"));
        assertTrue(summary.contains("杭州"));
        assertTrue(summary.contains("2天"));
        assertTrue(summary.contains("西湖"));
        assertTrue(summary.contains("灵隐寺"));
        assertTrue(summary.contains("乌镇"));
    }

    @Test
    void pageTripsForAdminDelegatesToJoinQuery() {
        AdminTripVO adminTrip = new AdminTripVO();
        adminTrip.setId(TRIP_ID);
        adminTrip.setUserId(USER_ID);
        adminTrip.setUsername("alice");
        Page<AdminTripVO> page = new Page<>(1, 10);
        page.setRecords(List.of(adminTrip));
        page.setTotal(1L);
        doReturn(page).when(tripMapper).selectAdminTripPage(any());

        PageResult<AdminTripVO> result = tripService.pageTripsForAdmin(1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("alice", result.getRecords().get(0).getUsername());
        assertEquals(1L, result.getTotal());
        verify(tripMapper, times(1)).selectAdminTripPage(any());
    }

    // ---------- helpers ----------

    private void stubTripInsert() {
        when(tripMapper.insert(any(Trip.class))).thenAnswer(invocation -> {
            Trip saved = invocation.getArgument(0);
            saved.setId(TRIP_ID);
            return 1;
        });
    }

    private GenerateTripRequest req(String city, int days) {
        return req(city, days, List.of("美食", "自然"));
    }

    private GenerateTripRequest req(String city, int days, List<String> preferences) {
        GenerateTripRequest req = new GenerateTripRequest();
        req.setCity(city);
        req.setDays(days);
        req.setBudget("2000-3000");
        req.setSpendMode("舒适");
        req.setPreferences(preferences);
        return req;
    }

    private static ScenicBrief brief(Long id, String name) {
        return new ScenicBrief(id, name, "杭州", "自然", "标签", "简介", "全天",
                new BigDecimal("0.00"));
    }

    private static Trip trip(Long id, Long userId) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setUserId(userId);
        trip.setTitle("杭州2日休闲之旅");
        trip.setCity("杭州");
        trip.setDays(2);
        trip.setPreferences("美食,自然");
        trip.setStatus(1);
        trip.setCreateAt(LocalDateTime.now());
        return trip;
    }

    private static TripDay tripDay(int dayNo, int id) {
        TripDay day = new TripDay();
        day.setId((long) id);
        day.setTripId(TRIP_ID);
        day.setDayNo(dayNo);
        day.setTheme("主题" + dayNo);
        return day;
    }

    private static TripSpot tripSpot(int dayNo, int visitOrder, int id, String name) {
        TripSpot spot = new TripSpot();
        spot.setId((long) id);
        spot.setTripId(TRIP_ID);
        spot.setDayNo(dayNo);
        spot.setVisitOrder(visitOrder);
        spot.setSpotName(name);
        spot.setStayMinutes(60);
        return spot;
    }

    private static TripSpot findSpot(List<TripSpot> spots, int dayNo, String name) {
        return spots.stream()
                .filter(s -> s.getDayNo() == dayNo && name.equals(s.getSpotName()))
                .findFirst()
                .orElseThrow();
    }

    private static Page<Trip> pageOf(Trip trip, long total) {
        Page<Trip> page = new Page<>(1, 10);
        page.setRecords(List.of(trip));
        page.setTotal(total);
        return page;
    }
}
