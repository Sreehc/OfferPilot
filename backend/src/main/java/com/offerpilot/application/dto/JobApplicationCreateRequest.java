package com.offerpilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import lombok.Data;

@Data
public class JobApplicationCreateRequest {
    @NotBlank(message = "cannot be blank")
    private String company;

    @NotBlank(message = "cannot be blank")
    private String jobTitle;

    private String city;

    private String source;

    @NotBlank(message = "cannot be blank")
    private String jdText;

    private Long resumeFileId;

    private LocalDate applyDate;
}
