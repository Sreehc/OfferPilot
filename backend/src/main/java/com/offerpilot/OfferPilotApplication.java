package com.offerpilot;

import com.offerpilot.ai.config.EmbeddingProperties;
import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.config.VectorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan("com.offerpilot.**.mapper")
@EnableConfigurationProperties({LlmProperties.class, EmbeddingProperties.class, VectorProperties.class})
public class OfferPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfferPilotApplication.class, args);
    }
}

