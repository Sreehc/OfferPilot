package com.offerpilot.ai.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiCallLogCommand {
    private Long userId;
    private String provider;
    private String model;
    private String callType;
    private String scene;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private String usageSource;
    private Long latencyMs;
    private boolean success;
    private String errorMessage;
}
