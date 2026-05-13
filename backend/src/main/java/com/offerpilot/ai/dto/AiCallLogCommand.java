package com.offerpilot.ai.dto;

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
    private Long latencyMs;
    private boolean success;
    private String errorMessage;
}
