package com.offerpilot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "offerpilot.ai.llm")
public class LlmProperties {
    private boolean enabled = false;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer timeoutSeconds = 30;
}

