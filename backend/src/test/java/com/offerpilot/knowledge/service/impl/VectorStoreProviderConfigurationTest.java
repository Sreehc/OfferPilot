package com.offerpilot.knowledge.service.impl;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.offerpilot.ai.config.VectorProperties;
import com.offerpilot.knowledge.service.VectorStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class VectorStoreProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VectorStoreProviderConfiguration.class))
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "offerpilot.ai.embedding.enabled=true",
                    "offerpilot.ai.vector.provider=postgres",
                    "offerpilot.ai.vector.dimensions=1536",
                    "offerpilot.ai.vector.initialize-schema=false",
                    "offerpilot.ai.vector.postgres.url=jdbc:postgresql://localhost:15432/offerpilot_rag",
                    "offerpilot.ai.vector.postgres.username=offerpilot",
                    "offerpilot.ai.vector.postgres.password=offerpilot");

    @Test
    void postgresProvider_createsPostgresVectorStoreService() {
        contextRunner.run(context -> {
            VectorStoreService vectorStoreService = context.getBean(VectorStoreService.class);
            assertInstanceOf(PostgresVectorStoreService.class, vectorStoreService);
        });
    }

    @Configuration
    @EnableConfigurationProperties(VectorProperties.class)
    static class TestConfiguration {
    }
}
