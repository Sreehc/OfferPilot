package com.offerpilot.admin.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminInterviewGovernanceSummaryVO {
    private long totalSessions;
    private long finishedSessions;
    private long voiceSessions;
    private long resumeProjectSessions;
    private BigDecimal averageScore;
}
