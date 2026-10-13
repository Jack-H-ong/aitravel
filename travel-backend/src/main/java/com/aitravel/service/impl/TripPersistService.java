package com.aitravel.service.impl;

import com.aitravel.dto.GenerateTripRequest;
import com.aitravel.entity.Trip;
import com.aitravel.entity.TripDay;
import com.aitravel.entity.TripSpot;
import com.aitravel.mapper.TripDayMapper;
import com.aitravel.mapper.TripMapper;
import com.aitravel.mapper.TripSpotMapper;
import com.aitravel.service.ScenicService;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.service.ai.TripPlan;
import com.aitravel.service.ai.TripPlanDay;
import com.aitravel.service.ai.TripPlanSpot;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 行程落库独立 Bean（详细设计 §5.5）。
 *
 * <p>{@code savePlan} 独立成 Bean，使 {@code @Transactional} 经 Spring 代理生效（避免与
 * {@code TripServiceImpl#generateTrip} 同类自调用导致注解失效）；AI 调用留在事务外。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的 Mapper 与 ScenicService 由 Spring 容器持有，此处仅保存引用")
public class TripPersistService {

    private final TripMapper tripMapper;
    private final TripDayMapper tripDayMapper;
    private final TripSpotMapper tripSpotMapper;
    private final ScenicService scenicService;

    /**
     * 构造落库服务。
     *
     * @param tripMapper     行程主表数据访问
     * @param tripDayMapper  每日行程数据访问
     * @param tripSpotMapper 行程景点数据访问
     * @param scenicService  景点服务（景点名 → id 映射）
     */
    public TripPersistService(TripMapper tripMapper, TripDayMapper tripDayMapper,
                              TripSpotMapper tripSpotMapper, ScenicService scenicService) {
        this.tripMapper = tripMapper;
        this.tripDayMapper = tripDayMapper;
        this.tripSpotMapper = tripSpotMapper;
        this.scenicService = scenicService;
    }

    /**
     * 在单个事务内落库 AI 生成的行程：插入 {@code trip}（回填 ID）、逐日插入 {@code trip_day}
     * 与 {@code trip_spot}，并按「景点名 → id」映射回填 {@code scenic_id}（匹配不到为 {@code null}）。
     *
     * @param userId 归属用户 ID
     * @param req    生成请求（城市用于二次取景点、偏好拼接入库）
     * @param plan   AI 生成的结构化行程
     * @return 新行程 ID
     */
    @Transactional
    public Long savePlan(Long userId, GenerateTripRequest req, TripPlan plan) {
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setTitle(plan.title());
        trip.setCity(req.getCity());
        trip.setDays(req.getDays());
        trip.setBudget(req.getBudget());
        trip.setSpendMode(req.getSpendMode());
        trip.setPreferences(join(req.getPreferences()));
        trip.setStatus(1);
        tripMapper.insert(trip);

        Map<String, Long> nameToScenicId = scenicService.listByCity(req.getCity()).stream()
                .collect(Collectors.toMap(ScenicBrief::name, ScenicBrief::id, (a, b) -> a));

        for (TripPlanDay day : plan.days()) {
            tripDayMapper.insert(TripDay.from(trip.getId(), day));
            for (TripPlanSpot spot : day.spots()) {
                TripSpot entity = TripSpot.from(trip.getId(), day.day(), spot,
                        nameToScenicId.get(spot.name()));
                tripSpotMapper.insert(entity);
            }
        }
        return trip.getId();
    }

    /**
     * 将偏好列表拼接为逗号分隔字符串。
     *
     * @param values 偏好列表，可为 {@code null}
     * @return 逗号分隔字符串；入参为 {@code null} 时返回 {@code null}
     */
    private static String join(List<String> values) {
        return values == null ? null : String.join(",", values);
    }
}
