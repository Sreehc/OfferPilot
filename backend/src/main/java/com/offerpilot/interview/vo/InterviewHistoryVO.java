package com.offerpilot.interview.vo;

import com.offerpilot.common.vo.ContextSourceVO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewHistoryVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private Integer durationMinutes;
    private Boolean includeResumeProject;
    private String contextType;
    private ContextSourceVO contextSource;
    private String status;
    private String mode;
    private BigDecimal totalScore;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean cardsGenerated;
    private Integer generatedCardCount;
    private String interviewDeckId;
}
