package com.offerpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRenameSessionRequest {
    @NotBlank(message = "cannot be blank")
    private String title;
}
