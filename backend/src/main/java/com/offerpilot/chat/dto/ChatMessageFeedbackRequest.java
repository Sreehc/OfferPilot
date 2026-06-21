package com.offerpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageFeedbackRequest {
    @NotBlank(message = "cannot be blank")
    private String feedback;
}
