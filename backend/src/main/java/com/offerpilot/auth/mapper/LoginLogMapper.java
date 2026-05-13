package com.offerpilot.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offerpilot.auth.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
