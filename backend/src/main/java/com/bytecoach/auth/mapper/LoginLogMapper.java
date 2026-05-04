package com.bytecoach.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bytecoach.auth.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
