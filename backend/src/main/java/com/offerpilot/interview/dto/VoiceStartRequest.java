package com.offerpilot.interview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VoiceStartRequest {
    @NotBlank(message = "cannot be blank")
    private String direction;

    private String jobRole;

    private String experienceLevel;

    private String techStack;

    @Min(value = 5, message = "must be at least 5")
    @Max(value = 60, message = "must be at most 60")
    private Integer durationMinutes = 20;

    private Boolean includeResumeProject = Boolean.FALSE;

    @Min(value = 1, message = "must be at least 1")
    @Max(value = 5, message = "must be at most 5")
    private Integer questionCount = 3;

    /** If set, this question will be included as the first question. */
    private Long reanswerQuestionId;
}
