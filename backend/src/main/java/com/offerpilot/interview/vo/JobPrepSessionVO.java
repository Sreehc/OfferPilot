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
public class JobPrepSessionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeFileId;
    private String resumeTitle;
    private String company;
    private String jobTitle;
    private String jdText;
    private String status;
    private BigDecimal matchScore;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private List<String> focusAreas;
    private List<String> resumeTalkingPoints;
    private List<String> mockQuestions;
    private List<String> nextActions;
    private String providerStatus;
    private String providerStatusMessage;
    private List<ProviderReadinessVO> providerReadiness;
    private String summary;
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
}
