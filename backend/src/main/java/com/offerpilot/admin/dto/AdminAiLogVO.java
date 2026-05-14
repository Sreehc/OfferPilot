package com.offerpilot.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminAiLogVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
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
    private Integer success;
    private String errorMessage;
    private LocalDateTime createTime;
}
