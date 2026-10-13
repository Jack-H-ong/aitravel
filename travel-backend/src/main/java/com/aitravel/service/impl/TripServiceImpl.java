package com.aitravel.service.impl;

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
import com.aitravel.service.ScenicService;
import com.aitravel.service.TripService;
import com.aitravel.service.ai.AiGateway;
import com.aitravel.service.ai.AiServiceException;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.service.ai.TripPlan;
import com.aitravel.service.ai.TripPlanRequest;
import com.aitravel.vo.AdminTripVO;
import com.aitravel.vo.TripDayVO;
import com.aitravel.vo.TripDetailVO;
import com.aitravel.vo.TripSpotVO;
import com.aitravel.vo.TripVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 行程服务实现（详细设计 §5.2 / §5.5 / §5.6）。
 *
 * <p>生成攻略时 AI 调用位于事务外，落库委托给独立 Bean {@link TripPersistService}；删除按
 * <b>D-01</b> 采用「主表逻辑删除 + 子表物理删除」。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 Mapper 与上游服务由 Spring 容器持有，此处仅保存引用")
public class TripServiceImpl implements TripService {

    /** 默认页码。 */
    private static final int DEFAULT_PAGE = 1;
    /** 默认每页大小。 */
    private static final int DEFAULT_SIZE = 10;
    /** 每页大小上限。 */
    private static final int MAX_SIZE = 100;
    /** 景点缺省停留分钟数。 */
    private static final Integer DEFAULT_STAY_MINUTES = 60;

    private final TripMapper tripMapper;
    private final TripDayMapper tripDayMapper;
    private final TripSpotMapper tripSpotMapper;
    private final ScenicService scenicService;
    private final AiGateway aiGateway;
    private final TripPersistService tripPersistService;

    /**
     * 构造服务实现。
     *
     * @param tripMapper        行程主表数据访问
     * @param tripDayMapper     每日行程数据访问
     * @param tripSpotMapper    行程景点数据访问
     * @param scenicService     景点服务
     * @param aiGateway         AI 网关
     * @param tripPersistService 行程落库独立 Bean
     */
    public TripServiceImpl(TripMapper tripMapper, TripDayMapper tripDayMapper,
                           TripSpotMapper tripSpotMapper, ScenicService scenicService,
                           AiGateway aiGateway, TripPersistService tripPersistService) {
        this.tripMapper = tripMapper;
        this.tripDayMapper = tripDayMapper;
        this.tripSpotMapper = tripSpotMapper;
        this.scenicService = scenicService;
        this.aiGateway = aiGateway;
        this.tripPersistService = tripPersistService;
    }

    @Override
    public TripDetailVO generateTrip(Long userId, GenerateTripRequest req) {
        // 1. 取景点（模块外调用，事务外）
        List<ScenicBrief> scenicList = scenicService.listByCity(req.getCity());
        if (scenicList.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, ErrorMessage.NO_SCENIC_IN_CITY);
        }

        // 2. 调 AI（HTTP，事务外，避免长事务占用连接）
        TripPlan plan;
        try {
            plan = aiGateway.generateTripPlan(new TripPlanRequest(req.getCity(), req.getDays(),
                    req.getBudget(), req.getSpendMode(), req.getPreferences(), scenicList));
        } catch (AiServiceException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, ErrorMessage.AI_GENERATE_FAILED);
        }

        // 3. 事务内落库（独立 Bean，避免 @Transactional 自调用失效）
        Long tripId = tripPersistService.savePlan(userId, req, plan);
        return getTripDetail(userId, tripId);
    }

    @Override
    public PageResult<TripVO> pageTrips(Long userId, String city, int page, int size) {
        Page<Trip> result = tripMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)),
                new LambdaQueryWrapper<Trip>()
                        .eq(Trip::getUserId, userId)
                        .eq(StringUtils.hasText(city), Trip::getCity, city)
                        .orderByDesc(Trip::getCreateAt));
        List<TripVO> records = result.getRecords().stream()
                .map(TripVO::from)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public TripDetailVO getTripDetail(Long userId, Long tripId) {
        Trip trip = requireOwnedTrip(tripId, userId);

        List<TripDay> days = new ArrayList<>(tripDayMapper.selectList(
                new LambdaQueryWrapper<TripDay>()
                        .eq(TripDay::getTripId, tripId)
                        .orderByAsc(TripDay::getDayNo)));
        days.sort(Comparator.comparing(TripDay::getDayNo));

        List<TripSpot> spots = new ArrayList<>(tripSpotMapper.selectList(
                new LambdaQueryWrapper<TripSpot>()
                        .eq(TripSpot::getTripId, tripId)
                        .orderByAsc(TripSpot::getDayNo, TripSpot::getVisitOrder)));
        spots.sort(Comparator.comparing(TripSpot::getDayNo)
                .thenComparing(TripSpot::getVisitOrder));

        Map<Integer, List<TripSpot>> spotsByDay = spots.stream()
                .collect(Collectors.groupingBy(TripSpot::getDayNo, LinkedHashMap::new,
                        Collectors.toList()));

        TripDetailVO vo = TripDetailVO.from(trip);
        List<TripDayVO> dayList = new ArrayList<>();
        for (TripDay day : days) {
            TripDayVO dayVo = TripDayVO.from(day);
            List<TripSpotVO> spotVos = spotsByDay.getOrDefault(day.getDayNo(), List.of()).stream()
                    .map(TripSpotVO::from)
                    .collect(Collectors.toList());
            dayVo.setSpots(spotVos);
            dayList.add(dayVo);
        }
        vo.setDayList(dayList);
        return vo;
    }

    @Override
    @Transactional
    public void updateTrip(Long userId, Long tripId, UpdateTripRequest req) {
        Trip trip = requireOwnedTrip(tripId, userId);

        if (StringUtils.hasText(req.getTitle())) {
            trip.setTitle(req.getTitle());
            tripMapper.updateById(trip);
        }

        // 子表无 deleted 字段，物理删除后重建
        tripDayMapper.delete(new LambdaQueryWrapper<TripDay>().eq(TripDay::getTripId, tripId));
        tripSpotMapper.delete(new LambdaQueryWrapper<TripSpot>().eq(TripSpot::getTripId, tripId));

        if (req.getDayList() == null) {
            return;
        }
        for (UpdateTripRequest.UpdateTripDay day : req.getDayList()) {
            TripDay dayEntity = new TripDay();
            dayEntity.setTripId(tripId);
            dayEntity.setDayNo(day.getDayNo());
            dayEntity.setTheme(day.getTheme());
            dayEntity.setLunch(day.getLunch());
            dayEntity.setDinner(day.getDinner());
            dayEntity.setTip(day.getTip());
            tripDayMapper.insert(dayEntity);

            if (day.getSpots() == null) {
                continue;
            }
            for (UpdateTripRequest.UpdateTripSpot spot : day.getSpots()) {
                TripSpot spotEntity = new TripSpot();
                spotEntity.setTripId(tripId);
                spotEntity.setScenicId(spot.getScenicId());
                spotEntity.setDayNo(day.getDayNo());
                spotEntity.setSpotName(spot.getSpotName());
                spotEntity.setVisitOrder(spot.getVisitOrder());
                Integer stayMinutes = spot.getStayMinutes();
                if (stayMinutes == null) {
                    stayMinutes = DEFAULT_STAY_MINUTES;
                }
                spotEntity.setStayMinutes(stayMinutes);
                spotEntity.setTip(spot.getTip());
                tripSpotMapper.insert(spotEntity);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTrip(Long userId, Long tripId) {
        requireOwnedTrip(tripId, userId);

        // D-01：主表逻辑删除，子表物理删除
        tripMapper.deleteById(tripId);
        tripDayMapper.delete(new LambdaQueryWrapper<TripDay>().eq(TripDay::getTripId, tripId));
        tripSpotMapper.delete(new LambdaQueryWrapper<TripSpot>().eq(TripSpot::getTripId, tripId));
    }

    @Override
    public String getTripSummary(Long tripId, Long userId) {
        TripDetailVO detail = getTripDetail(userId, tripId);
        StringBuilder summary = new StringBuilder();
        summary.append("【").append(detail.getTitle()).append("】")
                .append(detail.getCity()).append(" · ").append(detail.getDays()).append("天");
        for (TripDayVO day : detail.getDayList()) {
            summary.append("\n第").append(day.getDayNo()).append("天");
            if (StringUtils.hasText(day.getTheme())) {
                summary.append(" ").append(day.getTheme());
            }
            List<String> names = day.getSpots().stream()
                    .map(TripSpotVO::getSpotName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (!names.isEmpty()) {
                summary.append("：").append(String.join("、", names));
            }
        }
        return summary.toString();
    }

    @Override
    public PageResult<AdminTripVO> pageTripsForAdmin(int page, int size) {
        IPage<AdminTripVO> result = tripMapper.selectAdminTripPage(
                new Page<>(normalizePage(page), normalizeSize(size)));
        return new PageResult<>(result.getRecords(), result.getTotal(), result.getCurrent(),
                result.getSize());
    }

    /**
     * 归属校验（详情/修改/删除共用，§5.6）：不存在抛 404，不属于当前用户抛 403。
     *
     * @param tripId 行程 ID
     * @param userId 当前用户 ID
     * @return 已校验归属的行程实体
     */
    private Trip requireOwnedTrip(Long tripId, Long userId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorMessage.TRIP_NOT_FOUND);
        }
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, ErrorMessage.TRIP_FORBIDDEN);
        }
        return trip;
    }

    /**
     * 归一化页码：非法值回退默认页。
     *
     * @param page 原始页码
     * @return 合法页码
     */
    private static int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 归一化每页大小：非法值回退默认值，超上限截断。
     *
     * @param size 原始大小
     * @return 合法大小
     */
    private static int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
