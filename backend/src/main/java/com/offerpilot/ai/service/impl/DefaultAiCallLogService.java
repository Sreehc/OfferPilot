package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.dto.AiCallLogCommand;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.ai.service.AiCallLogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAiCallLogService implements AiCallLogService {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final AiCallLogMapper aiCallLogMapper;

    @Override
    public void record(AiCallLogCommand command) {
        try {
            AiCallLog logEntry = new AiCallLog();
            Integer promptTokens = firstNonNull(command.getPromptTokens(), command.getInputTokens(), 0);
            Integer completionTokens = firstNonNull(command.getCompletionTokens(), command.getOutputTokens(), 0);
            Integer totalTokens = firstNonNull(command.getTotalTokens(), sum(promptTokens, completionTokens), 0);
            logEntry.setUserId(command.getUserId());
            logEntry.setProvider(command.getProvider());
            logEntry.setModel(command.getModel());
            logEntry.setCallType(command.getCallType());
            logEntry.setScene(command.getScene());
            logEntry.setInputTokens(command.getInputTokens());
            logEntry.setOutputTokens(command.getOutputTokens());
            logEntry.setPromptTokens(promptTokens);
            logEntry.setCompletionTokens(completionTokens);
            logEntry.setTotalTokens(totalTokens);
            logEntry.setEstimatedCost(command.getEstimatedCost() == null
                    ? estimateCost(command.getCallType(), promptTokens, completionTokens, totalTokens)
                    : command.getEstimatedCost());
            logEntry.setUsageSource(command.getUsageSource() == null ? "estimated" : command.getUsageSource());
            logEntry.setLatencyMs(command.getLatencyMs());
            logEntry.setSuccess(command.isSuccess() ? 1 : 0);
            logEntry.setErrorMessage(trim(command.getErrorMessage()));
            aiCallLogMapper.insert(logEntry);
        } catch (Exception exception) {
            log.warn("Failed to persist ai call log: {}", exception.getMessage());
        }
    }

    private String trim(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }

    private Integer firstNonNull(Integer primary, Integer fallback, Integer defaultValue) {
        if (primary != null) {
            return primary;
        }
        if (fallback != null) {
            return fallback;
        }
        return defaultValue;
    }

    private Integer sum(Integer left, Integer right) {
        return (left == null ? 0 : left) + (right == null ? 0 : right);
    }

    private BigDecimal estimateCost(String callType, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        BigDecimal promptRatePerThousand = switch (callType == null ? "" : callType) {
            case "embedding" -> new BigDecimal("0.00010");
            default -> new BigDecimal("0.00050");
        };
        BigDecimal completionRatePerThousand = switch (callType == null ? "" : callType) {
            case "stream", "chat" -> new BigDecimal("0.00150");
            case "embedding" -> BigDecimal.ZERO;
            default -> new BigDecimal("0.00100");
        };
        int prompt = promptTokens == null ? 0 : promptTokens;
        int completion = completionTokens == null ? 0 : completionTokens;
        if (prompt == 0 && completion == 0 && totalTokens != null) {
            prompt = totalTokens;
        }
        return promptRatePerThousand.multiply(BigDecimal.valueOf(prompt))
                .add(completionRatePerThousand.multiply(BigDecimal.valueOf(completion)))
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP);
    }
}
