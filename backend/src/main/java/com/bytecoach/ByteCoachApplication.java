package com.bytecoach;

import com.bytecoach.ai.config.EmbeddingProperties;
import com.bytecoach.ai.config.LlmProperties;
import com.bytecoach.ai.config.VectorProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.bytecoach.**.mapper")
@EnableConfigurationProperties({LlmProperties.class, EmbeddingProperties.class, VectorProperties.class})
public class ByteCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(ByteCoachApplication.class, args);
    }
}

