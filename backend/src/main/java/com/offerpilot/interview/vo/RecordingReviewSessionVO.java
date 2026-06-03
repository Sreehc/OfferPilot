package com.offerpilot.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class RecordingReviewSessionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String direction;
    private String jobRole;
    private String notes;
    private String status;
    private String statusMessage;
    private String transcript;
    private BigDecimal transcriptConfidence;
    private Integer transcriptTimeMs;
    private BigDecimal overallScore;
    private String summary;
    private List<String> strengths;
    private List<String> weakPoints;
    private List<String> suggestedActions;
    private String providerStatus;
    private String providerStatusMessage;
    private String suggestedAgentType;
    private String suggestedTriggerSource;
    private String nextActionLabel;
    private String nextActionPath;
    private List<ProviderReadinessVO> providerReadiness;
    private List<SegmentVO> segments;
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
    public static class SegmentVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private Integer segmentIndex;
        private String transcriptText;
        private Integer startOffsetMs;
        private Integer endOffsetMs;
        private String signalType;
    }
}
