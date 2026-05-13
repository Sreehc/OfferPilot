package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.dto.AiChatRequest;
import com.offerpilot.ai.dto.AiChatResponse;
import com.offerpilot.ai.service.LlmGateway;
import com.offerpilot.ai.service.LlmQuotaService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
    private final LlmQuotaService llmQuotaService;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public AiChatResponse chatCompletion(AiChatRequest request) {
        checkQuotaAndConfig();

        int timeoutSeconds = llmProperties.getTimeoutSeconds() != null ? llmProperties.getTimeoutSeconds() : 30;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient restClient = restClientBuilder
                .baseUrl(trimBaseUrl(llmProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        List<Map<String, String>> messages = buildMessages(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", llmProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);

        Map<String, Object> response = null;
        try {
            response = callLlm(restClient, body);
        } catch (HttpServerErrorException e) {
            log.warn("LLM call failed with {}, retrying once...", e.getStatusCode());
            response = callLlm(restClient, body);
        }

        String content = extractContent(response);
        return AiChatResponse.builder().content(content).raw(String.valueOf(response)).build();
    }

    @Override
    public AiChatResponse streamCompletion(AiChatRequest request, Consumer<String> onToken) {
        checkQuotaAndConfig();

        List<Map<String, String>> messages = buildMessages(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", llmProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("stream", true);

        String url = trimBaseUrl(llmProperties.getBaseUrl()) + "/chat/completions";
        StringBuilder fullContent = new StringBuilder();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + llmProperties.getApiKey());
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            conn.setRequestProperty(HttpHeaders.ACCEPT, "text/event-stream");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(60_000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
                os.flush();
            }

            int httpCode = conn.getResponseCode();
            if (httpCode != 200) {
                log.warn("Streaming LLM call returned HTTP {}, falling back to non-streaming", httpCode);
                return chatCompletion(request);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                        String token = extractDeltaContent(chunk);
                        if (token != null && !token.isEmpty()) {
                            fullContent.append(token);
                            onToken.accept(token);
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse SSE chunk: {}", data);
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            log.warn("Streaming failed ({}), falling back to non-streaming", e.getMessage());
            return chatCompletion(request);
        }

        return AiChatResponse.builder()
                .content(fullContent.toString())
                .raw("stream")
                .build();
    }

    private void checkQuotaAndConfig() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            llmQuotaService.checkAndConsume(userId);
        }
        if (!llmProperties.isEnabled()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "LLM is not enabled");
        }
        if (!StringUtils.hasText(llmProperties.getBaseUrl())
                || !StringUtils.hasText(llmProperties.getApiKey())
                || !StringUtils.hasText(llmProperties.getModel())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "LLM configuration is incomplete");
        }
    }

    private List<Map<String, String>> buildMessages(AiChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        if (request.getReferences() != null && !request.getReferences().isEmpty()) {
            messages.add(Map.of("role", "system", "content", "References:\n" + String.join("\n", request.getReferences())));
        }
        messages.add(Map.of("role", "user", "content", request.getUserPrompt()));
        return messages;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> firstMap && firstMap.get("message") instanceof Map<?, ?> message) {
                Object maybeContent = message.get("content");
                return maybeContent == null ? "" : String.valueOf(maybeContent);
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractDeltaContent(Map<String, Object> chunk) {
        if (chunk.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                Object delta = firstMap.get("delta");
                if (delta instanceof Map<?, ?> deltaMap) {
                    Object content = deltaMap.get("content");
                    return content == null ? null : String.valueOf(content);
                }
            }
        }
        return null;
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
