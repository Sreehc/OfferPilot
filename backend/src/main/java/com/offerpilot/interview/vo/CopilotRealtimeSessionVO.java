package com.offerpilot.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CopilotRealtimeSessionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long copilotPrepSessionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeFileId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPrepSessionId;
    private String resumeTitle;
    private String company;
    private String jobTitle;
    private String status;
    private String providerStatus;
    private String prepSummary;
    private List<String> liveChecklist;
    private List<ProviderReadinessVO> providerReadiness;
    private String latestEventSummary;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private LocalDateTime endedAt;
    private PostInterviewReviewVO postInterviewReview;
    private List<CopilotRealtimeEventVO> events;
    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderReadinessVO {
        private String scope;
        private String label;
        private String status;
        private String statusMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopilotRealtimeEventVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long sessionId;
        private String eventType;
        private String source;
        private String summary;
        private Map<String, Object> payload;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInterviewReviewVO {
        private String summary;
        private List<String> strengths;
        private List<String> weakPoints;
        private List<String> recommendedActions;
        private String suggestedAgentType;
        private String suggestedTriggerSource;
        private String nextActionLabel;
        private String nextActionPath;
    }
}
