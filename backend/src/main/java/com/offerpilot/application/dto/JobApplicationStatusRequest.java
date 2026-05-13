package com.offerpilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

@Data
public class JobApplicationStatusRequest {
    @NotBlank(message = "cannot be blank")
    private String status;

    private String note;

    private LocalDate nextStepDate;
}
