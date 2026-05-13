package com.offerpilot.ai.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiChatRequest {
    private String systemPrompt;
    private String userPrompt;
    private List<String> references;
}

