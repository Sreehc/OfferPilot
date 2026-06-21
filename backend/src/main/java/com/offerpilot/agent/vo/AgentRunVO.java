package com.offerpilot.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentRunVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String agentType;
    private String triggerSource;
    private String status;
    private String title;
    private String summary;
    private String userPrompt;
    private List<String> contextRefs;
    private String streamMode;
    private List<String> recommendations;
    private List<String> checkpoints;
    private String nextActionPath;
    private String nextActionLabel;
    private Boolean requiresApproval;
    private String approvalActionType;
    private String approvalSummary;
    private String decisionNote;
    private String executionSummary;
    private String executionActionLabel;
    private String executionActionPath;
    private String approvalStage;
    private String approvalStatus;
    private String providerGateStatus;
    private String providerGateSummary;
    private List<TimelineItemVO> timeline;
    private List<TimelineItemVO> steps;
    private List<ToolCallVO> toolCalls;
    private List<ArtifactVO> artifacts;
    private List<ProviderGateVO> providerGates;
    private LocalDateTime updateTime;

    @Data
    @Builder
    public static class TimelineItemVO {
        private String key;
        private String stepType;
        private String title;
        private String description;
        private String status;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class ProviderGateVO {
        private String scope;
        private String label;
        private String status;
        private String statusMessage;
        private Boolean required;
    }

    @Data
    @Builder
    public static class ToolCallVO {
        private String id;
        private String name;
        private String status;
        private String startedAt;
        private String endedAt;
        private Long totalDurationMs;
        private Map<String, Long> phaseDurations;
        private Integer retryCount;
        private String inputSummary;
        private String outputSummary;
        private String errorType;
        private String errorMessage;
        private String rawErrorStack;
    }

    @Data
    @Builder
    public static class ArtifactVO {
        private String id;
        private String title;
        private String content;
        private String type;
        private String actionUrl;
    }
}
