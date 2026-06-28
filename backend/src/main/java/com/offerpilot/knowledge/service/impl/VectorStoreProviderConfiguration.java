package com.offerpilot.knowledge.service.impl;

import com.offerpilot.ai.config.VectorProperties;
import com.offerpilot.knowledge.service.VectorStoreService;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreProviderConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "offerpilot.ai.vector", name = "provider", havingValue = "postgres", matchIfMissing = true)
    public VectorStoreService postgresVectorStoreService(VectorProperties vectorProperties) {
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(vectorProperties.getPostgres().getUrl())
                .username(vectorProperties.getPostgres().getUsername())
                .password(vectorProperties.getPostgres().getPassword())
                .build();
        return new PostgresVectorStoreService(vectorProperties, dataSource);
    }
}
