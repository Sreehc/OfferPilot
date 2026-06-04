package com.offerpilot.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminRuntimeGovernanceSummaryVO {
    private long totalAgentRuns;
    private long pendingApprovalRuns;
    private long rejectedAgentRuns;
    private long providerBlockedRuns;

    private long totalCopilotPrepSessions;
    private long totalCopilotRealtimeSessions;
    private long liveCopilotRealtimeSessions;
    private long disconnectedCopilotRealtimeSessions;
    private long blockedCopilotRealtimeSessions;
    private long degradedCopilotRealtimeSessions;

    private long totalRecordingReviews;
    private long processingRecordingReviews;
    private long failedRecordingReviews;
    private long readyRecordingReviews;
    private Long avgTranscriptTimeMs;

    private long totalProviderConfigs;
    private long enabledProviderConfigs;
    private long readyProviderConfigs;
    private long failedProviderConfigs;
    private long uncheckedProviderConfigs;
    private long configuredProviderUsers;

    private BigDecimal totalEstimatedAiCost;
    private long failedAiCalls;
    private long avgAiLatencyMs;

    private List<String> riskSignals;
    private List<String> recommendations;
}
