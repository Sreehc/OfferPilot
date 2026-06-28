package com.offerpilot.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "offerpilot.ai.vector")
public class VectorProperties {
    private String provider = "postgres";
    private Integer dimensions = 1536;
    private boolean initializeSchema = true;
    private double similarityThreshold = 0.3;

    private Postgres postgres = new Postgres();

    @Data
    public static class Postgres {
        private String url;
        private String username;
        private String password;
        private String schema = "public";
        private String table = "knowledge_embedding";
    }
}
