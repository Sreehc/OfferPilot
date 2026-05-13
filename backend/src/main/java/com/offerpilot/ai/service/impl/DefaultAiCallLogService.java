package com.offerpilot.ai.service.impl;

import com.offerpilot.ai.dto.AiCallLogCommand;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.ai.service.AiCallLogService;
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
            logEntry.setUserId(command.getUserId());
            logEntry.setProvider(command.getProvider());
            logEntry.setModel(command.getModel());
            logEntry.setCallType(command.getCallType());
            logEntry.setScene(command.getScene());
            logEntry.setInputTokens(command.getInputTokens());
            logEntry.setOutputTokens(command.getOutputTokens());
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
}
