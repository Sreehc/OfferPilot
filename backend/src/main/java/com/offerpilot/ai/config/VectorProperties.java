package com.offerpilot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "offerpilot.ai.vector")
public class VectorProperties {
    private String indexName = "offerpilot_chunks";
    private double similarityThreshold = 0.3;
}
