package com.offerpilot.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminInterviewGovernanceVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private String mode;
    private String status;
    private BigDecimal totalScore;
    private Integer questionCount;
    private Integer durationMinutes;
    private boolean includeResumeProject;
    private Integer lowScoreCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
