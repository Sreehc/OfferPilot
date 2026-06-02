package com.offerpilot.agent.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class AgentRunCreateRequest {
    @NotBlank(message = "agentType is required")
    private String agentType;

    @NotBlank(message = "triggerSource is required")
    private String triggerSource;

    private List<String> contextRefs;

    private String streamMode;

    private String userPrompt;
}
