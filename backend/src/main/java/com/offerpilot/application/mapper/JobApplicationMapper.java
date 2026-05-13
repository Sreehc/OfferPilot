package com.offerpilot.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.offerpilot.application.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
