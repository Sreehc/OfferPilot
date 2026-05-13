package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.config.EmbeddingProperties;
import com.offerpilot.ai.dto.AiCallLogCommand;
import com.offerpilot.ai.service.AiCallLogService;
import com.offerpilot.ai.service.EmbeddingGateway;
import com.offerpilot.ai.service.SystemConfigService;
import com.offerpilot.ai.support.AiSystemConfigKeys;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
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
    private final AiCallLogService aiCallLogService;
    private final SystemConfigService systemConfigService;

    @Override
    public float[] embed(String text) {
        return embed(text, "knowledge.search");
    }

    @Override
    public float[] embed(String text, String scene) {
        List<float[]> results = embedBatch(List.of(text), scene);
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts) {
        return embedBatch(texts, "knowledge.index");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<float[]> embedBatch(List<String> texts, String scene) {
        if (!systemConfigService.getBoolean(AiSystemConfigKeys.EMBEDDING_ENABLED, embeddingProperties.isEnabled())) {
            return texts.stream().map(t -> new float[0]).toList();
        }
        String baseUrl = systemConfigService.getString(AiSystemConfigKeys.EMBEDDING_BASE_URL, embeddingProperties.getBaseUrl());
        String model = systemConfigService.getString(AiSystemConfigKeys.EMBEDDING_MODEL, embeddingProperties.getModel());
        Integer dimensions = systemConfigService.getInteger(AiSystemConfigKeys.EMBEDDING_DIMENSIONS, embeddingProperties.getDimensions());
        if (!StringUtils.hasText(baseUrl)
                || !StringUtils.hasText(embeddingProperties.getApiKey())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Embedding configuration is incomplete");
        }
        long startedAt = System.currentTimeMillis();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        RestClient restClient = restClientBuilder
                .baseUrl(trimBaseUrl(baseUrl))
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("input", texts);
        if (dimensions != null) {
            body.put("dimensions", dimensions);
        }

        try {
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
            while (embeddings.size() < texts.size()) {
                embeddings.add(new float[0]);
            }
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(SecurityUtils.getCurrentUserId())
                    .provider("openai-compatible")
                    .model(model)
                    .callType("embedding")
                    .scene(scene)
                    .inputTokens(estimateTokens(texts))
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(true)
                    .build());
            log.info("Embedding request: model={}, batchSize={}, dimensions={}", model, texts.size(), dimensions);
            return embeddings;
        } catch (RuntimeException exception) {
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(SecurityUtils.getCurrentUserId())
                    .provider("openai-compatible")
                    .model(model)
                    .callType("embedding")
                    .scene(scene)
                    .inputTokens(estimateTokens(texts))
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(false)
                    .errorMessage(exception.getMessage())
                    .build());
            throw exception;
        }
    }

    private Integer estimateTokens(List<String> texts) {
        int characters = texts.stream().mapToInt(text -> text == null ? 0 : text.length()).sum();
        if (characters <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(characters / 4.0));
    }

    private String trimBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
