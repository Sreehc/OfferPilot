package com.offerpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {
    private Long sessionId;

    @NotBlank(message = "cannot be blank")
    private String mode;

    @NotBlank(message = "cannot be blank")
    private String message;
}

