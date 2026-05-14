package com.offerpilot.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerpilot.admin.dto.AdminAiLogSummaryVO;
import com.offerpilot.admin.dto.AdminAiLogVO;
import com.offerpilot.admin.dto.AdminSystemConfigHistoryVO;
import com.offerpilot.admin.dto.AdminSystemConfigUpdateRequest;
import com.offerpilot.admin.dto.AdminSystemConfigVO;
import com.offerpilot.admin.dto.AdminUsageSummaryVO;
import com.offerpilot.admin.service.AdminAiGovernanceService;
import com.offerpilot.ai.config.EmbeddingProperties;
import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.entity.SystemConfig;
import com.offerpilot.ai.entity.SystemConfigHistory;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.ai.mapper.SystemConfigHistoryMapper;
import com.offerpilot.ai.service.SystemConfigService;
import com.offerpilot.ai.support.AiSystemConfigKeys;
import com.offerpilot.ai.support.PromptTemplateDefaults;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAiGovernanceServiceImpl implements AdminAiGovernanceService {

    private final AiCallLogMapper aiCallLogMapper;
    private final SystemConfigHistoryMapper systemConfigHistoryMapper;
    private final SystemConfigService systemConfigService;
    private final LlmProperties llmProperties;
    private final EmbeddingProperties embeddingProperties;

    @Override
    public PageResult<AdminAiLogVO> listAiLogs(String scene, String callType, Integer success, String keyword, int pageNum, int pageSize) {
        Page<AiCallLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiCallLog> queryWrapper = new LambdaQueryWrapper<AiCallLog>()
                .eq(StringUtils.hasText(scene), AiCallLog::getScene, scene)
                .eq(StringUtils.hasText(callType), AiCallLog::getCallType, callType)
                .eq(success != null, AiCallLog::getSuccess, success)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(AiCallLog::getModel, keyword)
                        .or()
                        .like(AiCallLog::getScene, keyword)
                        .or()
                        .like(AiCallLog::getErrorMessage, keyword))
                .orderByDesc(AiCallLog::getCreateTime);
        IPage<AiCallLog> result = aiCallLogMapper.selectPage(page, queryWrapper);
        List<AdminAiLogVO> records = result.getRecords().stream()
                .map(logEntry -> AdminAiLogVO.builder()
                        .id(logEntry.getId())
                        .userId(logEntry.getUserId())
                        .provider(logEntry.getProvider())
                        .model(logEntry.getModel())
                        .callType(logEntry.getCallType())
                        .scene(logEntry.getScene())
                        .inputTokens(logEntry.getInputTokens())
                        .outputTokens(logEntry.getOutputTokens())
                        .promptTokens(readPromptTokens(logEntry))
                        .completionTokens(readCompletionTokens(logEntry))
                        .totalTokens(readTotalTokens(logEntry))
                        .estimatedCost(logEntry.getEstimatedCost())
                        .usageSource(logEntry.getUsageSource())
                        .latencyMs(logEntry.getLatencyMs())
                        .success(logEntry.getSuccess())
                        .errorMessage(logEntry.getErrorMessage())
                        .createTime(logEntry.getCreateTime())
                        .build())
                .toList();
        return PageResult.<AdminAiLogVO>builder()
                .records(records)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) result.getPages())
                .build();
    }

    @Override
    public AdminAiLogSummaryVO aiLogSummary() {
        long total = aiCallLogMapper.selectCount(null);
        long success = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>().eq(AiCallLog::getSuccess, 1));
        long failed = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>().eq(AiCallLog::getSuccess, 0));
        long chatCalls = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>()
                .in(AiCallLog::getCallType, List.of("chat", "stream")));
        long embeddingCalls = aiCallLogMapper.selectCount(new LambdaQueryWrapper<AiCallLog>()
                .eq(AiCallLog::getCallType, "embedding"));
        List<AiCallLog> allLogs = aiCallLogMapper.selectList(new LambdaQueryWrapper<AiCallLog>()
                .orderByDesc(AiCallLog::getCreateTime));
        List<AiCallLog> recentLogs = aiCallLogMapper.selectList(new LambdaQueryWrapper<AiCallLog>()
                .isNotNull(AiCallLog::getLatencyMs)
                .orderByDesc(AiCallLog::getCreateTime)
                .last("LIMIT 200"));
        long avgLatency = recentLogs.isEmpty()
                ? 0
                : Math.round(recentLogs.stream()
                        .map(AiCallLog::getLatencyMs)
                        .filter(value -> value != null && value >= 0)
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0));
        long promptTokens = allLogs.stream().mapToLong(this::readPromptTokens).sum();
        long completionTokens = allLogs.stream().mapToLong(this::readCompletionTokens).sum();
        long totalTokens = allLogs.stream().mapToLong(this::readTotalTokens).sum();
        BigDecimal estimatedCost = allLogs.stream()
                .map(AiCallLog::getEstimatedCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return AdminAiLogSummaryVO.builder()
                .totalCalls(total)
                .successCalls(success)
                .failedCalls(failed)
                .avgLatencyMs(avgLatency)
                .chatCalls(chatCalls)
                .embeddingCalls(embeddingCalls)
                .usageSummary(AdminUsageSummaryVO.builder()
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .totalTokens(totalTokens)
                        .estimatedCost(estimatedCost)
                        .build())
                .build();
    }

    @Override
    public List<AdminSystemConfigVO> listSystemConfigs() {
        Map<String, SystemConfig> storedConfigs = systemConfigService.listAll().stream()
                .collect(java.util.stream.Collectors.toMap(SystemConfig::getConfigKey, value -> value, (left, right) -> right));
        Map<String, List<AdminSystemConfigHistoryVO>> configHistory = loadRecentConfigHistory();
        return knownConfigs().values().stream()
                .map(definition -> {
                    SystemConfig stored = storedConfigs.get(definition.key());
                    return AdminSystemConfigVO.builder()
                            .configGroup(definition.group())
                            .configKey(definition.key())
                            .displayName(definition.displayName())
                            .description(definition.description())
                            .valueType(definition.valueType())
                            .configValue(stored == null ? definition.runtimeDefault() : stored.getConfigValue())
                            .enabled(stored == null ? true : stored.getEnabled() == null || stored.getEnabled() == 1)
                            .runtimeDefault(definition.runtimeDefault())
                            .configHistory(configHistory.getOrDefault(definition.key(), List.of()))
                            .build();
                })
                .toList();
    }

    @Override
    public AdminSystemConfigVO updateSystemConfig(String configKey, AdminSystemConfigUpdateRequest request) {
        ConfigDefinition definition = knownConfigs().get(configKey);
        if (definition == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unknown system config key: " + configKey);
        }
        SystemConfig previous = systemConfigService.listAll().stream()
                .filter(item -> configKey.equals(item.getConfigKey()))
                .findFirst()
                .orElse(null);
        SystemConfig config = systemConfigService.save(
                definition.group(),
                definition.key(),
                request.getConfigValue(),
                definition.valueType(),
                definition.description(),
                Boolean.TRUE.equals(request.getEnabled()));
        writeHistory(config, previous, request.getChangeReason());
        return AdminSystemConfigVO.builder()
                .configGroup(config.getConfigGroup())
                .configKey(config.getConfigKey())
                .displayName(definition.displayName())
                .description(definition.description())
                .valueType(definition.valueType())
                .configValue(config.getConfigValue())
                .enabled(config.getEnabled() == null || config.getEnabled() == 1)
                .runtimeDefault(definition.runtimeDefault())
                .configHistory(loadRecentConfigHistory().getOrDefault(config.getConfigKey(), List.of()))
                .build();
    }

    private Map<String, List<AdminSystemConfigHistoryVO>> loadRecentConfigHistory() {
        List<SystemConfigHistory> historyRecords = systemConfigHistoryMapper.selectList(new LambdaQueryWrapper<SystemConfigHistory>()
                .orderByDesc(SystemConfigHistory::getCreateTime)
                .last("LIMIT 200"));
        Map<String, List<AdminSystemConfigHistoryVO>> grouped = new LinkedHashMap<>();
        historyRecords.stream()
                .map(item -> AdminSystemConfigHistoryVO.builder()
                        .id(item.getId())
                        .configGroup(item.getConfigGroup())
                        .configKey(item.getConfigKey())
                        .oldValue(item.getOldValue())
                        .newValue(item.getNewValue())
                        .oldEnabled(item.getOldEnabled() == null || item.getOldEnabled() == 1)
                        .newEnabled(item.getNewEnabled() == null || item.getNewEnabled() == 1)
                        .operatorUserId(item.getOperatorUserId())
                        .changeReason(item.getChangeReason())
                        .createTime(item.getCreateTime())
                        .build())
                .forEach(history -> grouped.computeIfAbsent(history.getConfigKey(), key -> new java.util.ArrayList<>()).add(history));
        grouped.replaceAll((key, value) -> value.stream().limit(5).toList());
        return grouped;
    }

    private void writeHistory(SystemConfig current, SystemConfig previous, String changeReason) {
        String currentValue = current == null ? null : current.getConfigValue();
        Integer currentEnabled = current == null ? null : current.getEnabled();
        String previousValue = previous == null ? null : previous.getConfigValue();
        Integer previousEnabled = previous == null ? null : previous.getEnabled();
        boolean sameValue = java.util.Objects.equals(previousValue, currentValue);
        boolean sameEnabled = java.util.Objects.equals(previousEnabled, currentEnabled);
        if (sameValue && sameEnabled) {
            return;
        }
        SystemConfigHistory history = new SystemConfigHistory();
        history.setConfigGroup(current.getConfigGroup());
        history.setConfigKey(current.getConfigKey());
        history.setOldValue(previousValue);
        history.setNewValue(currentValue);
        history.setOldEnabled(previousEnabled == null ? 1 : previousEnabled);
        history.setNewEnabled(currentEnabled == null ? 1 : currentEnabled);
        history.setOperatorUserId(SecurityUtils.getCurrentUserId());
        history.setChangeReason(StringUtils.hasText(changeReason) ? changeReason.trim() : "后台更新");
        systemConfigHistoryMapper.insert(history);
    }

    private int readPromptTokens(AiCallLog logEntry) {
        if (logEntry.getPromptTokens() != null) {
            return logEntry.getPromptTokens();
        }
        return logEntry.getInputTokens() == null ? 0 : logEntry.getInputTokens();
    }

    private int readCompletionTokens(AiCallLog logEntry) {
        if (logEntry.getCompletionTokens() != null) {
            return logEntry.getCompletionTokens();
        }
        return logEntry.getOutputTokens() == null ? 0 : logEntry.getOutputTokens();
    }

    private int readTotalTokens(AiCallLog logEntry) {
        if (logEntry.getTotalTokens() != null) {
            return logEntry.getTotalTokens();
        }
        return readPromptTokens(logEntry) + readCompletionTokens(logEntry);
    }

    private Map<String, ConfigDefinition> knownConfigs() {
        Map<String, ConfigDefinition> definitions = new LinkedHashMap<>();
        definitions.put(AiSystemConfigKeys.LLM_ENABLED, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_LLM)
                .key(AiSystemConfigKeys.LLM_ENABLED)
                .displayName("LLM 调用开关")
                .description("控制问答、面试评分、卡片生成等 LLM 能力是否启用。")
                .valueType("boolean")
                .runtimeDefault(String.valueOf(llmProperties.isEnabled()))
                .build());
        definitions.put(AiSystemConfigKeys.LLM_BASE_URL, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_LLM)
                .key(AiSystemConfigKeys.LLM_BASE_URL)
                .displayName("LLM Base URL")
                .description("OpenAI 兼容网关地址，支持通过后台覆盖默认配置。")
                .valueType("text")
                .runtimeDefault(llmProperties.getBaseUrl())
                .build());
        definitions.put(AiSystemConfigKeys.LLM_MODEL, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_LLM)
                .key(AiSystemConfigKeys.LLM_MODEL)
                .displayName("LLM 模型")
                .description("当前用于问答和评分的主模型名称。")
                .valueType("text")
                .runtimeDefault(llmProperties.getModel())
                .build());
        definitions.put(AiSystemConfigKeys.LLM_TIMEOUT_SECONDS, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_LLM)
                .key(AiSystemConfigKeys.LLM_TIMEOUT_SECONDS)
                .displayName("LLM 超时秒数")
                .description("请求网关的读超时时间，超过后会回退到默认响应。")
                .valueType("number")
                .runtimeDefault(String.valueOf(llmProperties.getTimeoutSeconds()))
                .build());
        definitions.put(AiSystemConfigKeys.EMBEDDING_ENABLED, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_EMBEDDING)
                .key(AiSystemConfigKeys.EMBEDDING_ENABLED)
                .displayName("Embedding 开关")
                .description("控制知识检索与向量化流程是否启用。")
                .valueType("boolean")
                .runtimeDefault(String.valueOf(embeddingProperties.isEnabled()))
                .build());
        definitions.put(AiSystemConfigKeys.EMBEDDING_BASE_URL, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_EMBEDDING)
                .key(AiSystemConfigKeys.EMBEDDING_BASE_URL)
                .displayName("Embedding Base URL")
                .description("Embedding 服务的兼容网关地址。")
                .valueType("text")
                .runtimeDefault(embeddingProperties.getBaseUrl())
                .build());
        definitions.put(AiSystemConfigKeys.EMBEDDING_MODEL, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_EMBEDDING)
                .key(AiSystemConfigKeys.EMBEDDING_MODEL)
                .displayName("Embedding 模型")
                .description("文档切分和检索使用的向量模型。")
                .valueType("text")
                .runtimeDefault(embeddingProperties.getModel())
                .build());
        definitions.put(AiSystemConfigKeys.EMBEDDING_DIMENSIONS, ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_EMBEDDING)
                .key(AiSystemConfigKeys.EMBEDDING_DIMENSIONS)
                .displayName("Embedding 维度")
                .description("向量维度，需与向量库和模型能力保持一致。")
                .valueType("number")
                .runtimeDefault(String.valueOf(embeddingProperties.getDimensions()))
                .build());
        definitions.put(AiSystemConfigKeys.PROMPT_CHAT, promptDefinition(AiSystemConfigKeys.PROMPT_CHAT, "通用问答 Prompt", "控制自由问答的基础系统提示词。", PromptTemplateDefaults.CHAT));
        definitions.put(AiSystemConfigKeys.PROMPT_KNOWLEDGE, promptDefinition(AiSystemConfigKeys.PROMPT_KNOWLEDGE, "知识库问答 Prompt", "控制 RAG 模式下的基础提示词。", PromptTemplateDefaults.KNOWLEDGE));
        definitions.put(AiSystemConfigKeys.PROMPT_REFERENCE_CONSTRAINT, promptDefinition(AiSystemConfigKeys.PROMPT_REFERENCE_CONSTRAINT, "引用约束 Prompt", "控制 RAG 命中引用后的回答约束。", PromptTemplateDefaults.REFERENCE_CONSTRAINT));
        definitions.put(AiSystemConfigKeys.PROMPT_INTERVIEW_SCORE, promptDefinition(AiSystemConfigKeys.PROMPT_INTERVIEW_SCORE, "面试评分 Prompt", "控制模拟面试评分、JSON 输出与弱项标签生成。", PromptTemplateDefaults.INTERVIEW_SCORE));
        definitions.put(AiSystemConfigKeys.PROMPT_FOLLOW_UP, promptDefinition(AiSystemConfigKeys.PROMPT_FOLLOW_UP, "追问 Prompt", "控制后续追问生成策略。", PromptTemplateDefaults.FOLLOW_UP));
        definitions.put(AiSystemConfigKeys.PROMPT_KNOWLEDGE_CARD, promptDefinition(AiSystemConfigKeys.PROMPT_KNOWLEDGE_CARD, "卡片生成 Prompt", "控制知识卡片结构化输出格式。", PromptTemplateDefaults.KNOWLEDGE_CARD));
        return definitions;
    }

    private ConfigDefinition promptDefinition(String key, String displayName, String description, String runtimeDefault) {
        return ConfigDefinition.builder()
                .group(AiSystemConfigKeys.GROUP_PROMPT)
                .key(key)
                .displayName(displayName)
                .description(description)
                .valueType("textarea")
                .runtimeDefault(runtimeDefault)
                .build();
    }

    @Builder
    private record ConfigDefinition(
            String group,
            String key,
            String displayName,
            String description,
            String valueType,
            String runtimeDefault) {
    }
}
