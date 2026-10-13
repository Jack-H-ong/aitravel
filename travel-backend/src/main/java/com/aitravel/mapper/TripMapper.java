package com.aitravel.mapper;

import com.aitravel.entity.Trip;
import com.aitravel.vo.AdminTripVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** {@code trip} 表数据访问。 */
@Mapper
public interface TripMapper extends BaseMapper<Trip> {

    /**
     * 后台分页查询行程并关联归属用户名（供 M-ADMIN）。
     *
     * <p>仅返回未逻辑删除的行程，按创建时间倒序；{@code user_id}/{@code create_at} 经
     * {@code map-underscore-to-camel-case} 映射为驼峰字段。
     *
     * @param page 分页参数（须为方法首参以触动物理分页）
     * @return 含用户名的行程分页
     */
    @Select("SELECT t.id, t.user_id, u.username, t.title, t.city, t.days, t.create_at "
            + "FROM trip t JOIN `user` u ON t.user_id = u.id "
            + "WHERE t.deleted = 0 ORDER BY t.create_at DESC")
    IPage<AdminTripVO> selectAdminTripPage(IPage<AdminTripVO> page);
}
