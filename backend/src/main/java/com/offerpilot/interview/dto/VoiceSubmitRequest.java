package com.offerpilot.interview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoiceSubmitRequest {
    @NotNull(message = "cannot be null")
    private Long sessionId;

    @NotNull(message = "cannot be null")
    private Long questionId;
}
