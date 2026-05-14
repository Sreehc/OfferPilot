package com.offerpilot.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminAiLogSummaryVO {
    private long totalCalls;
    private long successCalls;
    private long failedCalls;
    private long avgLatencyMs;
    private long chatCalls;
    private long embeddingCalls;
    private AdminUsageSummaryVO usageSummary;
}
