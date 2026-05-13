package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.dto.AiChatRequest;
import com.offerpilot.ai.dto.AiCallLogCommand;
import com.offerpilot.ai.dto.AiChatResponse;
import com.offerpilot.ai.service.AiCallLogService;
import com.offerpilot.ai.service.LlmGateway;
import com.offerpilot.ai.service.LlmQuotaService;
import com.offerpilot.ai.service.SystemConfigService;
import com.offerpilot.ai.support.AiSystemConfigKeys;
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
    private final AiCallLogService aiCallLogService;
    private final SystemConfigService systemConfigService;

    @Override
    @SuppressWarnings("unchecked")
    public AiChatResponse chatCompletion(AiChatRequest request) {
        checkQuotaAndConfig();
        long startedAt = System.currentTimeMillis();
        Long userId = SecurityUtils.getCurrentUserId();
        String provider = "openai-compatible";
        String model = resolveModel();
        Integer inputTokens = estimatePromptTokens(request);

        int timeoutSeconds = resolveTimeoutSeconds();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient restClient = restClientBuilder
                .baseUrl(trimBaseUrl(resolveBaseUrl()))
                .requestFactory(requestFactory)
                .build();

        List<Map<String, String>> messages = buildMessages(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);

        try {
            Map<String, Object> response = callLlm(restClient, body);
            String content = extractContent(response);
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(userId)
                    .provider(provider)
                    .model(model)
                    .callType("chat")
                    .scene(request.getScene())
                    .inputTokens(inputTokens)
                    .outputTokens(estimateTokens(content))
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(true)
                    .build());
            return AiChatResponse.builder().content(content).raw(String.valueOf(response)).build();
        } catch (HttpServerErrorException e) {
            log.warn("LLM call failed with {}, retrying once...", e.getStatusCode());
            try {
                Map<String, Object> response = callLlm(restClient, body);
                String content = extractContent(response);
                aiCallLogService.record(AiCallLogCommand.builder()
                        .userId(userId)
                        .provider(provider)
                        .model(model)
                        .callType("chat")
                        .scene(request.getScene())
                        .inputTokens(inputTokens)
                        .outputTokens(estimateTokens(content))
                        .latencyMs(System.currentTimeMillis() - startedAt)
                        .success(true)
                        .build());
                return AiChatResponse.builder().content(content).raw(String.valueOf(response)).build();
            } catch (RuntimeException retryException) {
                aiCallLogService.record(AiCallLogCommand.builder()
                        .userId(userId)
                        .provider(provider)
                        .model(model)
                        .callType("chat")
                        .scene(request.getScene())
                        .inputTokens(inputTokens)
                        .latencyMs(System.currentTimeMillis() - startedAt)
                        .success(false)
                        .errorMessage(retryException.getMessage())
                        .build());
                throw retryException;
            }
        } catch (RuntimeException exception) {
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(userId)
                    .provider(provider)
                    .model(model)
                    .callType("chat")
                    .scene(request.getScene())
                    .inputTokens(inputTokens)
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(false)
                    .errorMessage(exception.getMessage())
                    .build());
            throw exception;
        }
    }

    @Override
    public AiChatResponse streamCompletion(AiChatRequest request, Consumer<String> onToken) {
        checkQuotaAndConfig();
        long startedAt = System.currentTimeMillis();
        Long userId = SecurityUtils.getCurrentUserId();
        String provider = "openai-compatible";
        String model = resolveModel();
        Integer inputTokens = estimatePromptTokens(request);

        List<Map<String, String>> messages = buildMessages(request);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.3);
        body.put("stream", true);

        String url = trimBaseUrl(resolveBaseUrl()) + "/chat/completions";
        StringBuilder fullContent = new StringBuilder();

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + llmProperties.getApiKey());
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            conn.setRequestProperty(HttpHeaders.ACCEPT, "text/event-stream");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(resolveTimeoutSeconds() * 1000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
                os.flush();
            }

            int httpCode = conn.getResponseCode();
            if (httpCode != 200) {
                log.warn("Streaming LLM call returned HTTP {}, falling back to non-streaming", httpCode);
                aiCallLogService.record(AiCallLogCommand.builder()
                        .userId(userId)
                        .provider(provider)
                        .model(model)
                        .callType("stream")
                        .scene(request.getScene())
                        .inputTokens(inputTokens)
                        .latencyMs(System.currentTimeMillis() - startedAt)
                        .success(false)
                        .errorMessage("HTTP " + httpCode)
                        .build());
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
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(userId)
                    .provider(provider)
                    .model(model)
                    .callType("stream")
                    .scene(request.getScene())
                    .inputTokens(inputTokens)
                    .outputTokens(estimateTokens(fullContent.toString()))
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.warn("Streaming failed ({}), falling back to non-streaming", e.getMessage());
            aiCallLogService.record(AiCallLogCommand.builder()
                    .userId(userId)
                    .provider(provider)
                    .model(model)
                    .callType("stream")
                    .scene(request.getScene())
                    .inputTokens(inputTokens)
                    .latencyMs(System.currentTimeMillis() - startedAt)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build());
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
        if (!systemConfigService.getBoolean(AiSystemConfigKeys.LLM_ENABLED, llmProperties.isEnabled())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "LLM is not enabled");
        }
        if (!StringUtils.hasText(resolveBaseUrl())
                || !StringUtils.hasText(llmProperties.getApiKey())
                || !StringUtils.hasText(resolveModel())) {
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

    private String resolveBaseUrl() {
        return systemConfigService.getString(AiSystemConfigKeys.LLM_BASE_URL, llmProperties.getBaseUrl());
    }

    private String resolveModel() {
        return systemConfigService.getString(AiSystemConfigKeys.LLM_MODEL, llmProperties.getModel());
    }

    private int resolveTimeoutSeconds() {
        Integer timeout = systemConfigService.getInteger(AiSystemConfigKeys.LLM_TIMEOUT_SECONDS, llmProperties.getTimeoutSeconds());
        return timeout == null ? 30 : timeout;
    }

    private Integer estimatePromptTokens(AiChatRequest request) {
        int totalLength = safeLength(request.getSystemPrompt()) + safeLength(request.getUserPrompt());
        if (request.getReferences() != null) {
            totalLength += request.getReferences().stream().mapToInt(this::safeLength).sum();
        }
        return estimateTokens(totalLength);
    }

    private Integer estimateTokens(String text) {
        return estimateTokens(safeLength(text));
    }

    private Integer estimateTokens(int characters) {
        if (characters <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(characters / 4.0));
    }

    private int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private String trimBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
