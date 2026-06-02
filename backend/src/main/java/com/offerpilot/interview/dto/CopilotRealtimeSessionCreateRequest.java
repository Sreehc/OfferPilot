package com.offerpilot.interview.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CopilotRealtimeSessionCreateRequest {

    @NotNull(message = "copilotPrepSessionId is required")
    private Long copilotPrepSessionId;

    @Size(max = 500, message = "openingNote too long")
    private String openingNote;
}
