package com.offerpilot.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.offerpilot.admin.dto.AdminAiLogSummaryVO;
import com.offerpilot.admin.dto.AdminAiLogVO;
import com.offerpilot.admin.dto.AdminSystemConfigUpdateRequest;
import com.offerpilot.admin.dto.AdminSystemConfigVO;
import com.offerpilot.admin.service.AdminAiGovernanceService;
import com.offerpilot.ai.config.EmbeddingProperties;
import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.entity.SystemConfig;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.ai.service.SystemConfigService;
import com.offerpilot.ai.support.AiSystemConfigKeys;
import com.offerpilot.ai.support.PromptTemplateDefaults;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAiGovernanceServiceImpl implements AdminAiGovernanceService {

    private final AiCallLogMapper aiCallLogMapper;
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
        return AdminAiLogSummaryVO.builder()
                .totalCalls(total)
                .successCalls(success)
                .failedCalls(failed)
                .avgLatencyMs(avgLatency)
                .chatCalls(chatCalls)
                .embeddingCalls(embeddingCalls)
                .build();
    }

    @Override
    public List<AdminSystemConfigVO> listSystemConfigs() {
        Map<String, SystemConfig> storedConfigs = systemConfigService.listAll().stream()
                .collect(java.util.stream.Collectors.toMap(SystemConfig::getConfigKey, value -> value, (left, right) -> right));
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
        SystemConfig config = systemConfigService.save(
                definition.group(),
                definition.key(),
                request.getConfigValue(),
                definition.valueType(),
                definition.description(),
                Boolean.TRUE.equals(request.getEnabled()));
        return AdminSystemConfigVO.builder()
                .configGroup(config.getConfigGroup())
                .configKey(config.getConfigKey())
                .displayName(definition.displayName())
                .description(definition.description())
                .valueType(definition.valueType())
                .configValue(config.getConfigValue())
                .enabled(config.getEnabled() == null || config.getEnabled() == 1)
                .runtimeDefault(definition.runtimeDefault())
                .build();
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
