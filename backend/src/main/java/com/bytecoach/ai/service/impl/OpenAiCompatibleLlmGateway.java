package com.bytecoach.ai.service.impl;

import com.bytecoach.ai.config.LlmProperties;
import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.LlmGateway;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final LlmProperties llmProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    @SuppressWarnings("unchecked")
    public AiChatResponse chatCompletion(AiChatRequest request) {
        if (!llmProperties.isEnabled()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "LLM is not enabled");
        }
        if (!StringUtils.hasText(llmProperties.getBaseUrl())
                || !StringUtils.hasText(llmProperties.getApiKey())
                || !StringUtils.hasText(llmProperties.getModel())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "LLM configuration is incomplete");
        }

        int timeoutSeconds = llmProperties.getTimeoutSeconds() != null ? llmProperties.getTimeoutSeconds() : 30;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient restClient = restClientBuilder
                .baseUrl(trimBaseUrl(llmProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        if (request.getReferences() != null && !request.getReferences().isEmpty()) {
            messages.add(Map.of("role", "system", "content", "References:\n" + String.join("\n", request.getReferences())));
        }
        messages.add(Map.of("role", "user", "content", request.getUserPrompt()));

        Map<String, Object> body = new HashMap<>();
        body.put("model", llmProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);

        // Retry once on 5xx errors
        Map<String, Object> response = null;
        try {
            response = callLlm(restClient, body);
        } catch (HttpServerErrorException e) {
            log.warn("LLM call failed with {}, retrying once...", e.getStatusCode());
            response = callLlm(restClient, body);
        }

        String content = "";
        if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> firstMap && firstMap.get("message") instanceof Map<?, ?> message) {
                Object maybeContent = message.get("content");
                content = maybeContent == null ? "" : String.valueOf(maybeContent);
            }
        }
        return AiChatResponse.builder().content(content).raw(String.valueOf(response)).build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callLlm(RestClient restClient, Map<String, Object> body) {
        return restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + llmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);
    }

    private String trimBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
