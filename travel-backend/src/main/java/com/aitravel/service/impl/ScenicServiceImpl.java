package com.aitravel.service.impl;

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
import com.aitravel.service.ScenicService;
import com.aitravel.service.ai.ScenicBrief;
import com.aitravel.vo.BrowseHistoryVO;
import com.aitravel.vo.FavoriteVO;
import com.aitravel.vo.ScenicDetailVO;
import com.aitravel.vo.ScenicVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 景点服务实现（详细设计 §4.5）。
 *
 * <p>收藏切换在<b>同一事务</b>内维护 {@code favorite} 与 {@code favorite_count}，并发重复提交由唯一键
 * {@code uk_user_scenic} 兜底（D-03）；浏览记录 UPSERT 与浏览量自增<b>不参与事务</b>（D-02）。
 */
@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "DI 注入的三个 Mapper 由 Spring 容器持有，此处仅保存引用")
public class ScenicServiceImpl implements ScenicService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final ScenicSpotMapper scenicSpotMapper;
    private final FavoriteMapper favoriteMapper;
    private final BrowseHistoryMapper browseHistoryMapper;

    /**
     * 构造服务实现。
     *
     * @param scenicSpotMapper   景点数据访问
     * @param favoriteMapper     收藏数据访问
     * @param browseHistoryMapper 浏览记录数据访问
     */
    public ScenicServiceImpl(ScenicSpotMapper scenicSpotMapper, FavoriteMapper favoriteMapper,
                             BrowseHistoryMapper browseHistoryMapper) {
        this.scenicSpotMapper = scenicSpotMapper;
        this.favoriteMapper = favoriteMapper;
        this.browseHistoryMapper = browseHistoryMapper;
    }

    @Override
    public PageResult<ScenicVO> pageScenic(ScenicQuery query, Long userId) {
        Page<ScenicSpot> result = scenicSpotMapper.selectPage(
                new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())),
                buildWrapper(query));
        // 未登录不查 favorite 表（D-04）
        Set<Long> favoriteIds = (userId == null) ? Set.of() : loadFavoriteScenicIds(userId);
        List<ScenicVO> records = result.getRecords().stream()
                .map(spot -> ScenicVO.from(spot, favoriteIds.contains(spot.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public ScenicDetailVO detail(Long id, Long userId) {
        ScenicSpot spot = scenicSpotMapper.selectById(id);
        if (spot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorMessage.SCENIC_NOT_FOUND);
        }

        scenicSpotMapper.incrementViewCount(id);

        boolean isFavorite = false;
        if (userId != null) {
            browseHistoryMapper.upsert(userId, id);
            isFavorite = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .eq(Favorite::getScenicId, id)) > 0;
        }

        ScenicDetailVO vo = ScenicDetailVO.from(spot, isFavorite);
        // viewCount 返回自增后的值：selectById 已加载自增前的值，此处 +1
        Integer before = spot.getViewCount();
        vo.setViewCount((before == null ? 0 : before) + 1);
        return vo;
    }

    @Override
    @Transactional
    public FavoriteVO toggleFavorite(Long userId, Long scenicId) {
        ScenicSpot spot = scenicSpotMapper.selectById(scenicId);
        if (spot == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorMessage.SCENIC_NOT_FOUND);
        }

        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getScenicId, scenicId));

        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            scenicSpotMapper.decrementFavoriteCount(scenicId);
            return new FavoriteVO("unfavorite", false);
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setScenicId(scenicId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一键兜底，幂等返回且不再自增计数
            return new FavoriteVO("favorite", true);
        }
        scenicSpotMapper.incrementFavoriteCount(scenicId);
        return new FavoriteVO("favorite", true);
    }

    @Override
    public PageResult<ScenicVO> myFavorites(Long userId, int page, int size) {
        IPage<ScenicSpot> result = favoriteMapper.selectFavoriteScenicPage(
                new Page<>(normalizePage(page), normalizeSize(size)), userId);
        List<ScenicVO> records = result.getRecords().stream()
                .map(spot -> ScenicVO.from(spot, true))
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public List<ScenicBrief> listByCity(String city) {
        List<ScenicSpot> spots = scenicSpotMapper.selectList(
                new LambdaQueryWrapper<ScenicSpot>().eq(ScenicSpot::getCity, city));
        return spots.stream().map(ScenicServiceImpl::toBrief).collect(Collectors.toList());
    }

    @Override
    public List<ScenicBrief> listBriefByCity(String city) {
        return listByCity(city);
    }

    @Override
    public PageResult<BrowseHistoryVO> pageBrowseHistory(Long userId, int page, int size) {
        Page<BrowseHistory> result = browseHistoryMapper.selectPage(
                new Page<>(normalizePage(page), normalizeSize(size)),
                new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId)
                        .orderByDesc(BrowseHistory::getBrowseTime));

        List<BrowseHistory> histories = result.getRecords();
        List<Long> scenicIds = histories.stream()
                .map(BrowseHistory::getScenicId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, ScenicSpot> spotMap = new HashMap<>();
        if (!scenicIds.isEmpty()) {
            for (ScenicSpot spot : scenicSpotMapper.selectBatchIds(scenicIds)) {
                spotMap.put(spot.getId(), spot);
            }
        }

        List<BrowseHistoryVO> records = new ArrayList<>();
        for (BrowseHistory history : histories) {
            ScenicSpot spot = spotMap.get(history.getScenicId());
            if (spot != null) {
                records.add(BrowseHistoryVO.from(spot, history.getBrowseTime()));
            }
        }
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public PageResult<ScenicVO> pageSpotsForAdmin(ScenicQuery query) {
        Page<ScenicSpot> result = scenicSpotMapper.selectPage(
                new Page<>(normalizePage(query.getPage()), normalizeSize(query.getSize())),
                buildWrapper(query));
        List<ScenicVO> records = result.getRecords().stream()
                .map(ScenicVO::from)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public Long createSpot(ScenicSaveRequest req) {
        ScenicSpot spot = new ScenicSpot();
        applyRequest(spot, req);
        scenicSpotMapper.insert(spot);
        return spot.getId();
    }

    @Override
    @Transactional
    public void updateSpot(Long id, ScenicSaveRequest req) {
        ScenicSpot existing = scenicSpotMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, ErrorMessage.SCENIC_NOT_FOUND);
        }
        applyRequest(existing, req);
        scenicSpotMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteSpot(Long id) {
        scenicSpotMapper.deleteById(id);
    }

    /**
     * 构造景点动态查询条件：城市/分类精确匹配，关键字模糊匹配名称或简介。
     *
     * @param query 查询参数
     * @return 查询条件
     */
    private LambdaQueryWrapper<ScenicSpot> buildWrapper(ScenicQuery query) {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getCity()), ScenicSpot::getCity, query.getCity());
        wrapper.eq(StringUtils.hasText(query.getCategory()), ScenicSpot::getCategory,
                query.getCategory());
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(ScenicSpot::getName, keyword)
                    .or().like(ScenicSpot::getDescription, keyword));
        }
        return wrapper;
    }

    /**
     * 查询用户已收藏的景点 ID 集合。
     *
     * @param userId 用户 ID
     * @return 景点 ID 集合
     */
    private Set<Long> loadFavoriteScenicIds(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId));
        return favorites.stream()
                .map(Favorite::getScenicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 将请求字段写入实体；{@code tags}/{@code images} 拼接为逗号分隔字符串。
     *
     * @param spot 目标实体
     * @param req  请求
     */
    private void applyRequest(ScenicSpot spot, ScenicSaveRequest req) {
        spot.setName(req.getName());
        spot.setCity(req.getCity());
        spot.setAddress(req.getAddress());
        spot.setCategory(req.getCategory());
        spot.setPrice(req.getPrice());
        spot.setOpenTime(req.getOpenTime());
        spot.setDescription(req.getDescription());
        spot.setTags(join(req.getTags()));
        spot.setImages(join(req.getImages()));
    }

    /**
     * 将字符串列表拼接为逗号分隔字符串。
     *
     * @param values 字符串列表，可为 {@code null}
     * @return 逗号分隔字符串；入参为 {@code null} 时返回 {@code null}
     */
    private static String join(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    /**
     * 将景点实体映射为简要信息。
     *
     * @param spot 景点实体
     * @return 景点简要信息
     */
    private static ScenicBrief toBrief(ScenicSpot spot) {
        return new ScenicBrief(spot.getId(), spot.getName(), spot.getCity(), spot.getCategory(),
                spot.getTags(), spot.getDescription(), spot.getOpenTime(), spot.getPrice());
    }

    /**
     * 归一化页码：非法值回退默认页。
     *
     * @param page 原始页码，可为 {@code null}
     * @return 合法页码
     */
    private static int normalizePage(Integer page) {
        return (page == null || page < 1) ? DEFAULT_PAGE : page;
    }

    /**
     * 归一化每页大小：非法值回退默认值，超上限截断。
     *
     * @param size 原始大小，可为 {@code null}
     * @return 合法大小
     */
    private static int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
