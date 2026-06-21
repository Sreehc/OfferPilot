package com.offerpilot.admin.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminAiLogSummaryVO {
    private long totalCalls;
    private long successCalls;
    private long failedCalls;
    private double failureRate;
    private long avgLatencyMs;
    private long latencyP95Ms;
    private long chatCalls;
    private long embeddingCalls;
    private List<AdminErrorReasonBucketVO> errorReasonBuckets;
    private AdminUsageSummaryVO usageSummary;
    private AdminUsageSummaryVO costSummary;
}
