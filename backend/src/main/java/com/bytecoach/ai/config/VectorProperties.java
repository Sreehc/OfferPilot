package com.bytecoach.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bytecoach.ai.vector")
public class VectorProperties {
    private String indexName = "bytecoach_chunks";
    private double similarityThreshold = 0.3;
}
