package com.offerpilot.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class ChatSendRequest {
    private Long sessionId;

    private String clientMessageId;

    @NotBlank(message = "cannot be blank")
    private String mode = "chat";

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

    private List<String> attachmentIds;
}
