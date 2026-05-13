package com.offerpilot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "offerpilot.ai.embedding")
public class EmbeddingProperties {
    private boolean enabled = false;
    private String baseUrl;
    private String apiKey;
    private String model = "text-embedding-3-small";
    private Integer dimensions = 1536;
}
