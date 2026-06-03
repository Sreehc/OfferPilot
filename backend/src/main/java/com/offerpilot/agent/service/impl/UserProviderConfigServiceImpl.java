package com.offerpilot.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.agent.dto.ProviderConfigUpdateItemRequest;
import com.offerpilot.agent.entity.UserProviderConfig;
import com.offerpilot.agent.mapper.UserProviderConfigMapper;
import com.offerpilot.agent.provider.ProviderSecretCrypto;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserProviderConfigServiceImpl implements UserProviderConfigService {

    private static final Map<String, ScopeDefinition> DEFINITIONS = createDefinitions();

    private final UserProviderConfigMapper userProviderConfigMapper;
    private final ProviderSecretCrypto providerSecretCrypto;

    public UserProviderConfigServiceImpl(
            UserProviderConfigMapper userProviderConfigMapper,
            ProviderSecretCrypto providerSecretCrypto) {
        this.userProviderConfigMapper = userProviderConfigMapper;
        this.providerSecretCrypto = providerSecretCrypto;
    }

    @Override
    public List<UserProviderConfigItemVO> listCurrentUserConfigs() {
        Long userId = requireCurrentUserId();
        Map<String, UserProviderConfig> stored = userProviderConfigMapper.selectList(
                        new LambdaQueryWrapper<UserProviderConfig>()
                                .eq(UserProviderConfig::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(
                        item -> normalizeScope(item.getProviderScope()),
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));
        return DEFINITIONS.values().stream()
                .map(definition -> toView(definition, stored.get(definition.scope())))
                .toList();
    }

    @Override
    @Transactional
    public List<UserProviderConfigItemVO> updateCurrentUserConfigs(List<ProviderConfigUpdateItemRequest> requests) {
        Long userId = requireCurrentUserId();
        Map<String, UserProviderConfig> stored = userProviderConfigMapper.selectList(
                        new LambdaQueryWrapper<UserProviderConfig>()
                                .eq(UserProviderConfig::getUserId, userId))
                .stream()
                .collect(Collectors.toMap(
                        item -> normalizeScope(item.getProviderScope()),
                        item -> item,
                        (left, right) -> right,
                        LinkedHashMap::new));

        for (ProviderConfigUpdateItemRequest request : requests) {
            String scope = normalizeScope(request.getScope());
            ScopeDefinition definition = DEFINITIONS.get(scope);
            if (definition == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的 provider scope: " + request.getScope());
            }
            UserProviderConfig config = stored.computeIfAbsent(scope, ignored -> {
                UserProviderConfig created = new UserProviderConfig();
                created.setUserId(userId);
                created.setProviderScope(scope);
                created.setEnabled(Boolean.FALSE);
                created.setLastCheckStatus("unknown");
                return created;
            });
            applyRequest(config, request);
            refreshCheckState(config, definition, true);
            if (config.getId() == null) {
                userProviderConfigMapper.insert(config);
            } else {
                userProviderConfigMapper.updateById(config);
            }
        }

        return listCurrentUserConfigs();
    }

    @Override
    @Transactional
    public List<UserProviderConfigItemVO> checkCurrentUserConfigs() {
        Long userId = requireCurrentUserId();
        List<UserProviderConfig> stored = userProviderConfigMapper.selectList(
                new LambdaQueryWrapper<UserProviderConfig>()
                        .eq(UserProviderConfig::getUserId, userId));
        for (UserProviderConfig config : stored) {
            ScopeDefinition definition = DEFINITIONS.get(normalizeScope(config.getProviderScope()));
            if (definition == null) {
                continue;
            }
            refreshCheckState(config, definition, true);
            userProviderConfigMapper.updateById(config);
        }
        return listCurrentUserConfigs();
    }

    private void applyRequest(UserProviderConfig config, ProviderConfigUpdateItemRequest request) {
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        config.setProviderName(trimToNull(request.getProviderName()));
        config.setBaseUrl(trimToNull(request.getBaseUrl()));
        config.setModel(trimToNull(request.getModel()));
        config.setEndpoint(trimToNull(request.getEndpoint()));
        config.setBucket(trimToNull(request.getBucket()));
        config.setRegionName(trimToNull(request.getRegionName()));
        config.setDimensions(request.getDimensions());

        if (Boolean.TRUE.equals(request.getClearApiKey())) {
            config.setApiKeyCiphertext(null);
        } else if (StringUtils.hasText(request.getApiKey())) {
            config.setApiKeyCiphertext(providerSecretCrypto.encrypt(request.getApiKey().trim()));
        }

        if (Boolean.TRUE.equals(request.getClearAccessKey())) {
            config.setAccessKeyCiphertext(null);
        } else if (StringUtils.hasText(request.getAccessKey())) {
            config.setAccessKeyCiphertext(providerSecretCrypto.encrypt(request.getAccessKey().trim()));
        }

        if (Boolean.TRUE.equals(request.getClearSecretKey())) {
            config.setSecretKeyCiphertext(null);
        } else if (StringUtils.hasText(request.getSecretKey())) {
            config.setSecretKeyCiphertext(providerSecretCrypto.encrypt(request.getSecretKey().trim()));
        }
    }

    private void refreshCheckState(UserProviderConfig config, ScopeDefinition definition, boolean runProbe) {
        boolean configured = isConfigured(config, definition);
        config.setLastCheckedAt(LocalDateTime.now());
        if (!configured) {
            List<String> missingFields = definition.requiredFields().stream()
                    .filter(field -> isFieldMissing(config, field))
                    .map(this::fieldLabel)
                    .toList();
            config.setLastCheckStatus(Boolean.TRUE.equals(config.getEnabled()) ? "incomplete" : "unknown");
            config.setLastCheckMessage(missingFields.isEmpty()
                    ? "当前还没有可用配置。"
                    : "缺少 " + String.join("、", missingFields) + "。");
            return;
        }

        if (!Boolean.TRUE.equals(config.getEnabled())) {
            config.setLastCheckStatus("saved");
            config.setLastCheckMessage("配置已保存，启用后可供对应能力使用。");
            return;
        }

        if (!runProbe) {
            config.setLastCheckStatus("ready");
            config.setLastCheckMessage("配置完整，可供对应能力使用。");
            return;
        }

        ProbeResult probe = probeAvailability(config, definition);
        config.setLastCheckStatus(probe.success() ? "ready" : "failed");
        config.setLastCheckMessage(probe.message());
    }

    private UserProviderConfigItemVO toView(ScopeDefinition definition, UserProviderConfig config) {
        boolean configured = config != null && isConfigured(config, definition);
        return UserProviderConfigItemVO.builder()
                .scope(definition.scope())
                .label(definition.label())
                .description(definition.description())
                .enabled(config != null && Boolean.TRUE.equals(config.getEnabled()))
                .configured(configured)
                .status(resolveStatus(config, configured))
                .statusMessage(resolveStatusMessage(config, definition, configured))
                .providerName(config != null ? config.getProviderName() : null)
                .baseUrl(config != null ? config.getBaseUrl() : null)
                .model(config != null ? config.getModel() : null)
                .apiKeyMasked(config != null ? providerSecretCrypto.mask(config.getApiKeyCiphertext()) : "")
                .accessKeyMasked(config != null ? providerSecretCrypto.mask(config.getAccessKeyCiphertext()) : "")
                .secretKeyMasked(config != null ? providerSecretCrypto.mask(config.getSecretKeyCiphertext()) : "")
                .endpoint(config != null ? config.getEndpoint() : null)
                .bucket(config != null ? config.getBucket() : null)
                .regionName(config != null ? config.getRegionName() : null)
                .dimensions(config != null ? config.getDimensions() : null)
                .lastCheckedAt(config != null ? config.getLastCheckedAt() : null)
                .lastCheckStatus(config != null ? config.getLastCheckStatus() : "unknown")
                .lastCheckMessage(config != null ? config.getLastCheckMessage() : "还没有保存这类配置。")
                .updateTime(config != null ? config.getUpdateTime() : null)
                .build();
    }

    private String resolveStatus(UserProviderConfig config, boolean configured) {
        if (config == null) {
            return "missing";
        }
        if (Boolean.TRUE.equals(config.getEnabled()) && configured && "failed".equals(config.getLastCheckStatus())) {
            return "failed";
        }
        if (Boolean.TRUE.equals(config.getEnabled()) && configured) {
            return "ready";
        }
        if (configured) {
            return "saved";
        }
        return Boolean.TRUE.equals(config.getEnabled()) ? "incomplete" : "missing";
    }

    private String resolveStatusMessage(UserProviderConfig config, ScopeDefinition definition, boolean configured) {
        if (config == null) {
            return "还没有保存 " + definition.label() + " 配置。";
        }
        if (StringUtils.hasText(config.getLastCheckMessage())) {
            return config.getLastCheckMessage();
        }
        return configured ? "配置完整。" : "配置仍不完整。";
    }

    private boolean isConfigured(UserProviderConfig config, ScopeDefinition definition) {
        return definition.requiredFields().stream().noneMatch(field -> isFieldMissing(config, field));
    }

    private boolean isFieldMissing(UserProviderConfig config, String field) {
        return switch (field) {
            case "providerName" -> !StringUtils.hasText(config.getProviderName());
            case "baseUrl" -> !StringUtils.hasText(config.getBaseUrl());
            case "model" -> !StringUtils.hasText(config.getModel());
            case "apiKey" -> !StringUtils.hasText(providerSecretCrypto.decrypt(config.getApiKeyCiphertext()));
            case "accessKey" -> !StringUtils.hasText(providerSecretCrypto.decrypt(config.getAccessKeyCiphertext()));
            case "secretKey" -> !StringUtils.hasText(providerSecretCrypto.decrypt(config.getSecretKeyCiphertext()));
            case "endpoint" -> !StringUtils.hasText(config.getEndpoint());
            case "bucket" -> !StringUtils.hasText(config.getBucket());
            default -> true;
        };
    }

    private String fieldLabel(String field) {
        return switch (field) {
            case "providerName" -> "服务商";
            case "baseUrl" -> "Base URL";
            case "model" -> "模型";
            case "apiKey" -> "API Key";
            case "accessKey" -> "Access Key";
            case "secretKey" -> "Secret Key";
            case "endpoint" -> "Endpoint";
            case "bucket" -> "Bucket";
            default -> field;
        };
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }

    private String normalizeScope(String scope) {
        return trimToNull(scope) == null ? "" : trimToNull(scope).toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ProbeResult probeAvailability(UserProviderConfig config, ScopeDefinition definition) {
        String scope = normalizeScope(config.getProviderScope());
        String apiKey = providerSecretCrypto.decrypt(config.getApiKeyCiphertext());
        return switch (scope) {
            case "llm", "embedding" -> probeHttpEndpoint(
                    config.getBaseUrl(),
                    "/models",
                    apiKey,
                    true,
                    definition.label() + " 探测成功，可正常访问模型目录。");
            case "asr" -> probeHttpEndpoint(
                    config.getBaseUrl(),
                    "/audio/transcriptions",
                    apiKey,
                    false,
                    definition.label() + " 探测成功，转写接口地址可访问。");
            case "search", "voiceprint" -> probeHttpEndpoint(
                    config.getBaseUrl(),
                    "",
                    apiKey,
                    false,
                    definition.label() + " 探测成功，服务地址可访问。");
            case "oss" -> probeOssEndpoint(config, definition);
            default -> new ProbeResult(true, "配置完整，可供对应能力使用。");
        };
    }

    private ProbeResult probeOssEndpoint(UserProviderConfig config, ScopeDefinition definition) {
        try {
            URI endpoint = URI.create(config.getEndpoint());
            if (!StringUtils.hasText(endpoint.getScheme()) || !StringUtils.hasText(endpoint.getHost())) {
                return new ProbeResult(false, definition.label() + " Endpoint 不是有效地址，请检查协议和域名。");
            }
            return new ProbeResult(true, definition.label() + " 配置完整，已通过 Endpoint 与 Bucket 基础校验。");
        } catch (Exception ex) {
            return new ProbeResult(false, definition.label() + " Endpoint 无法解析，请检查地址格式。");
        }
    }

    private ProbeResult probeHttpEndpoint(
            String baseUrl,
            String probePath,
            String apiKey,
            boolean strictSuccessCode,
            String successMessage
    ) {
        String normalizedBaseUrl;
        try {
            normalizedBaseUrl = trimBaseUrl(baseUrl);
            URI.create(normalizedBaseUrl);
        } catch (Exception ex) {
            return new ProbeResult(false, "Base URL 无法解析，请检查地址格式。");
        }

        String targetUrl = normalizedBaseUrl + (StringUtils.hasText(probePath) ? probePath : "");
        try {
            int statusCode = executeProbeRequest(targetUrl, apiKey, "HEAD");
            if (statusCode == 405 || statusCode == 501) {
                statusCode = executeProbeRequest(targetUrl, apiKey, "GET");
            }
            if (statusCode == 401 || statusCode == 403) {
                return new ProbeResult(false, "服务地址可达，但认证失败，请检查 API Key。");
            }
            if (isReachableStatus(statusCode, strictSuccessCode)) {
                return new ProbeResult(true, successMessage);
            }
            if (statusCode >= 500) {
                return new ProbeResult(false, "服务地址可达，但当前返回 " + statusCode + "，请稍后重试。");
            }
            return new ProbeResult(false, "探测返回 HTTP " + statusCode + "，请确认 Base URL 是否指向正确接口。");
        } catch (Exception ex) {
            return new ProbeResult(false, "无法连接到配置的服务地址：" + simplifyProbeError(ex));
        }
    }

    private int executeProbeRequest(String targetUrl, String apiKey, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        connection.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        connection.setRequestProperty("Accept", "application/json");
        if (StringUtils.hasText(apiKey)) {
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        }
        connection.connect();
        int statusCode = connection.getResponseCode();
        connection.disconnect();
        return statusCode;
    }

    private boolean isReachableStatus(int statusCode, boolean strictSuccessCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return true;
        }
        if (statusCode == 429) {
            return true;
        }
        if (!strictSuccessCode && (statusCode == 404 || statusCode == 405)) {
            return true;
        }
        return false;
    }

    private String simplifyProbeError(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.length() > 120 ? message.substring(0, 120) + "..." : message;
    }

    private String trimBaseUrl(String baseUrl) {
        String normalized = trimToNull(baseUrl);
        if (normalized == null) {
            return "";
        }
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private static Map<String, ScopeDefinition> createDefinitions() {
        Map<String, ScopeDefinition> definitions = new LinkedHashMap<>();
        register(definitions, new ScopeDefinition("llm", "主模型", "配置通用问答、训练建议和 agent 任务使用的主 LLM。", Set.of("baseUrl", "model", "apiKey")));
        register(definitions, new ScopeDefinition("embedding", "向量模型", "配置知识库、简历、JD 和画像使用的 Embedding 服务。", Set.of("baseUrl", "model", "apiKey")));
        register(definitions, new ScopeDefinition("asr", "语音识别", "配置录音复盘和实时 Copilot 的语音转写服务。", Set.of("providerName", "baseUrl", "apiKey")));
        register(definitions, new ScopeDefinition("search", "联网搜索", "配置公司研究和岗位背景检索能力。", Set.of("providerName", "baseUrl", "apiKey")));
        register(definitions, new ScopeDefinition("oss", "对象存储", "配置长音频、录音文件等大对象上传能力。", Set.of("endpoint", "bucket", "accessKey", "secretKey")));
        register(definitions, new ScopeDefinition("voiceprint", "声纹识别", "配置实时 Copilot 的说话人识别能力。", Set.of("providerName", "baseUrl", "apiKey")));
        return definitions;
    }

    private static void register(Map<String, ScopeDefinition> definitions, ScopeDefinition definition) {
        definitions.put(definition.scope(), definition);
    }

    private static final class ScopeDefinition {
        private final String scope;
        private final String label;
        private final String description;
        private final Set<String> requiredFields;

        private ScopeDefinition(String scope, String label, String description, Set<String> requiredFields) {
            this.scope = scope;
            this.label = label;
            this.description = description;
            this.requiredFields = requiredFields;
        }

        private String scope() {
            return scope;
        }

        private String label() {
            return label;
        }

        private String description() {
            return description;
        }

        private Set<String> requiredFields() {
            return requiredFields;
        }
    }

    private record ProbeResult(boolean success, String message) {
    }
}
