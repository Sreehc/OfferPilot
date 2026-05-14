package com.offerpilot.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class JobApplicationEventCreateRequest {
    @NotBlank(message = "cannot be blank")
    private String eventType;

    @NotBlank(message = "cannot be blank")
    private String title;

    private String content;

    private LocalDateTime eventTime;

    private String result;

    private Integer interviewRound;

    private String interviewer;

    private List<String> feedbackTags;
}
