package com.offerpilot.ai.service;

import com.offerpilot.ai.dto.AiCallLogCommand;

public interface AiCallLogService {
    void record(AiCallLogCommand command);
}
