package com.offerpilot.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InterviewAnswerRequest {
    @NotNull(message = "cannot be null")
    private Long sessionId;

    @NotNull(message = "cannot be null")
    private Long questionId;

    @NotBlank(message = "cannot be blank")
    private String answer;

    /** Populated by service layer before passing to AI — not from client */
    private String questionTitle;
    private String standardAnswer;
    private String scoreStandard;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private Boolean includeResumeProject;
    private String contextType;
    private String contextSummary;
}
