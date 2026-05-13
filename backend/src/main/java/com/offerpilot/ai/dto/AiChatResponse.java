package com.offerpilot.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatResponse {
    private String content;
    private String raw;
}

