package com.offerpilot.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StudyPlanGenerateRequest {
    @Min(value = 7, message = "must be at least 7")
    @Max(value = 30, message = "must be at most 30")
    private Integer durationDays = 7;

    private String focusDirection;

    private String targetRole;

    private String techStack;
}
