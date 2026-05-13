package com.offerpilot.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("ai_call_log")
@EqualsAndHashCode(callSuper = true)
public class AiCallLog extends BaseEntity {
    private Long userId;
    private String provider;
    private String model;
    private String callType;
    private String scene;
    private Integer inputTokens;
    private Integer outputTokens;
    private Long latencyMs;
    private Integer success;
    private String errorMessage;
}
