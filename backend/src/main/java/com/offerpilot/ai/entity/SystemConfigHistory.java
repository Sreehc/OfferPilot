package com.offerpilot.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("system_config_history")
@EqualsAndHashCode(callSuper = true)
public class SystemConfigHistory extends BaseEntity {
    private String configGroup;
    private String configKey;
    private String oldValue;
    private String newValue;
    private Integer oldEnabled;
    private Integer newEnabled;
    private Long operatorUserId;
    private String changeReason;
}
