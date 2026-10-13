package com.aitravel.mapper;

import com.aitravel.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** {@code user} 表数据访问。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
