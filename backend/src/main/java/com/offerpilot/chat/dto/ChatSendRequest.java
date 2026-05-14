package com.offerpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatSendRequest {
    private Long sessionId;

    @NotBlank(message = "cannot be blank")
    private String mode;

    @NotBlank(message = "cannot be blank")
    private String answerMode = "learning";

    private String knowledgeScope = "all";

    private Long resumeId;

    private Long projectId;

    /** Populated by service layer when the current user is known. */
    private Long userId;

    /** Populated by service layer after resolving the bound context. */
    private String contextType;

    /** Populated by service layer after resolving the bound context. */
    private String contextSummary;

    @NotBlank(message = "cannot be blank")
    private String message;
}
