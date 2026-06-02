package com.offerpilot.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CopilotPrepSessionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeFileId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPrepSessionId;
    private String resumeTitle;
    private String company;
    private String jobTitle;
    private String jdText;
    private String notes;
    private String status;
    private String summary;
    private List<String> openingBrief;
    private List<String> keyRisks;
    private List<String> liveCues;
    private List<String> followUpQuestions;
    private List<String> nextActions;
    private List<ProviderReadinessVO> providerReadiness;
    private LocalDateTime updateTime;

    @Data
    @Builder
    public static class ProviderReadinessVO {
        private String scope;
        private String label;
        private String status;
        private String statusMessage;
    }
}
