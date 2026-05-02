package com.bytecoach.ai.service.impl;

import com.bytecoach.ai.config.EmbeddingProperties;
import com.bytecoach.ai.service.EmbeddingGateway;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiEmbeddingGateway implements EmbeddingGateway {

    private final EmbeddingProperties embeddingProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public float[] embed(String text) {
        List<float[]> results = embedBatch(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts) {
        if (!embeddingProperties.isEnabled()) {
            return texts.stream().map(t -> new float[0]).toList();
        }
        if (!StringUtils.hasText(embeddingProperties.getBaseUrl())
                || !StringUtils.hasText(embeddingProperties.getApiKey())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Embedding configuration is incomplete");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient restClient = restClientBuilder
                .baseUrl(trimBaseUrl(embeddingProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("model", embeddingProperties.getModel());
        body.put("input", texts);
        if (embeddingProperties.getDimensions() != null) {
            body.put("dimensions", embeddingProperties.getDimensions());
        }

        Map<String, Object> response = restClient.post()
                .uri("/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + embeddingProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        List<float[]> embeddings = new ArrayList<>();
        if (response != null && response.get("data") instanceof List<?> dataList) {
            for (Object item : dataList) {
                if (item instanceof Map<?, ?> dataMap && dataMap.get("embedding") instanceof List<?> vector) {
                    float[] arr = new float[vector.size()];
                    for (int i = 0; i < vector.size(); i++) {
                        arr[i] = ((Number) vector.get(i)).floatValue();
                    }
                    embeddings.add(arr);
                }
            }
        }
        // Ensure order matches input
        while (embeddings.size() < texts.size()) {
            embeddings.add(new float[0]);
        }
        log.info("Embedding request: model={}, batchSize={}, dimensions={}",
                embeddingProperties.getModel(), texts.size(), embeddingProperties.getDimensions());
        return embeddings;
    }

    private String trimBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
