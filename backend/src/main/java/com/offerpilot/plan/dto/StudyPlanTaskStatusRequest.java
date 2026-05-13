package com.offerpilot.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudyPlanTaskStatusRequest {
    @NotBlank(message = "cannot be blank")
    private String status;
}
