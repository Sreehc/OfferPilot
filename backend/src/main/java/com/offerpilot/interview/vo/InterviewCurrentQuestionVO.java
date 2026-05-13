package com.offerpilot.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewCurrentQuestionVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private Integer currentIndex;
    private Integer questionCount;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;
    private String questionTitle;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private Integer durationMinutes;
    private Boolean includeResumeProject;
}
