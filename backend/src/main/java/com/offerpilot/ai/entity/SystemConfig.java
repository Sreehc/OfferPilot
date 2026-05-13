package com.offerpilot.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("system_config")
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends BaseEntity {
    private String configGroup;
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;
    private Integer enabled;
}
