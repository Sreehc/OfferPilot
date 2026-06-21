package com.offerpilot.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.offerpilot.admin.dto.AdminAiLogSummaryVO;
import com.offerpilot.admin.service.impl.AdminAiGovernanceServiceImpl;
import com.offerpilot.ai.config.EmbeddingProperties;
import com.offerpilot.ai.config.LlmProperties;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.ai.mapper.SystemConfigHistoryMapper;
import com.offerpilot.ai.service.SystemConfigService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAiGovernanceServiceImplTest {

    @Mock
    private AiCallLogMapper aiCallLogMapper;
    @Mock
    private SystemConfigHistoryMapper systemConfigHistoryMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private LlmProperties llmProperties;
    @Mock
    private EmbeddingProperties embeddingProperties;

    @InjectMocks
    private AdminAiGovernanceServiceImpl service;

    @Test
    void aiLogSummary_returnsBackendGovernanceMetrics() {
        AiCallLog failedQuota = log("chat", 0, 100L, "quota exceeded", 100, 50, "0.12");
        AiCallLog failedQuotaAgain = log("stream", 0, 200L, "quota exceeded", 120, 60, "0.18");
        AiCallLog failedTimeout = log("embedding", 0, 300L, "timeout", 80, 10, "0.02");
        AiCallLog success = log("chat", 1, 400L, null, 160, 90, "0.20");
        AiCallLog slow = log("chat", 1, 800L, null, 0, 0, "0");

        when(aiCallLogMapper.selectCount(nullable(Wrapper.class))).thenReturn(5L, 2L, 3L, 3L, 1L);
        when(aiCallLogMapper.selectList(any())).thenReturn(
                List.of(failedQuota, failedQuotaAgain, failedTimeout, success, slow),
                List.of(failedQuota, failedQuotaAgain, failedTimeout, success, slow));

        AdminAiLogSummaryVO summary = service.aiLogSummary();

        assertEquals(5L, summary.getTotalCalls());
        assertEquals(60.0, summary.getFailureRate());
        assertEquals(800L, summary.getLatencyP95Ms());
        assertEquals(2, summary.getErrorReasonBuckets().get(0).getCount());
        assertEquals("quota exceeded", summary.getErrorReasonBuckets().get(0).getReason());
        assertEquals(new BigDecimal("0.52"), summary.getCostSummary().getEstimatedCost());
    }

    private AiCallLog log(String callType, int success, long latencyMs, String errorMessage,
                          int promptTokens, int completionTokens, String cost) {
        AiCallLog log = new AiCallLog();
        log.setCallType(callType);
        log.setSuccess(success);
        log.setLatencyMs(latencyMs);
        log.setErrorMessage(errorMessage);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setEstimatedCost(new BigDecimal(cost));
        return log;
    }
}
