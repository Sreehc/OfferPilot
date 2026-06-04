package com.offerpilot.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.admin.dto.AdminRuntimeGovernanceSummaryVO;
import com.offerpilot.admin.service.AdminRuntimeGovernanceService;
import com.offerpilot.agent.entity.AgentRun;
import com.offerpilot.agent.entity.UserProviderConfig;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.agent.mapper.UserProviderConfigMapper;
import com.offerpilot.ai.entity.AiCallLog;
import com.offerpilot.ai.mapper.AiCallLogMapper;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.CopilotRealtimeSession;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.CopilotRealtimeSessionMapper;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminRuntimeGovernanceServiceImpl implements AdminRuntimeGovernanceService {

    private final AgentRunMapper agentRunMapper;
    private final CopilotPrepSessionMapper copilotPrepSessionMapper;
    private final CopilotRealtimeSessionMapper copilotRealtimeSessionMapper;
    private final RecordingReviewSessionMapper recordingReviewSessionMapper;
    private final UserProviderConfigMapper userProviderConfigMapper;
    private final AiCallLogMapper aiCallLogMapper;

    @Override
    public AdminRuntimeGovernanceSummaryVO summary() {
        long totalAgentRuns = agentRunMapper.selectCount(null);
        long pendingApprovalRuns = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getStatus, "pending_approval"));
        long rejectedAgentRuns = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getStatus, "rejected"));
        long providerBlockedRuns = agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getNextActionPath, "/settings?tab=providers"));

        long totalCopilotPrepSessions = copilotPrepSessionMapper.selectCount(null);
        long totalCopilotRealtimeSessions = copilotRealtimeSessionMapper.selectCount(null);
        long liveCopilotRealtimeSessions = copilotRealtimeSessionMapper.selectCount(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getStatus, "live"));
        long disconnectedCopilotRealtimeSessions = copilotRealtimeSessionMapper.selectCount(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getStatus, "disconnected"));
        long blockedCopilotRealtimeSessions = copilotRealtimeSessionMapper.selectCount(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getProviderStatus, "blocked"));
        long degradedCopilotRealtimeSessions = copilotRealtimeSessionMapper.selectCount(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getProviderStatus, "degraded"));

        long totalRecordingReviews = recordingReviewSessionMapper.selectCount(null);
        long processingRecordingReviews = recordingReviewSessionMapper.selectCount(new LambdaQueryWrapper<RecordingReviewSession>()
                .eq(RecordingReviewSession::getStatus, "processing"));
        long failedRecordingReviews = recordingReviewSessionMapper.selectCount(new LambdaQueryWrapper<RecordingReviewSession>()
                .eq(RecordingReviewSession::getStatus, "failed"));
        long readyRecordingReviews = recordingReviewSessionMapper.selectCount(new LambdaQueryWrapper<RecordingReviewSession>()
                .eq(RecordingReviewSession::getStatus, "ready"));
        List<RecordingReviewSession> transcriptSessions = recordingReviewSessionMapper.selectList(new LambdaQueryWrapper<RecordingReviewSession>()
                .isNotNull(RecordingReviewSession::getTranscriptTimeMs)
                .orderByDesc(RecordingReviewSession::getUpdateTime)
                .last("LIMIT 200"));
        Long avgTranscriptTimeMs = transcriptSessions.isEmpty()
                ? null
                : Math.round(transcriptSessions.stream()
                        .map(RecordingReviewSession::getTranscriptTimeMs)
                        .filter(value -> value != null && value >= 0)
                        .mapToLong(Integer::longValue)
                        .average()
                        .orElse(0));

        List<UserProviderConfig> providerConfigs = userProviderConfigMapper.selectList(new LambdaQueryWrapper<UserProviderConfig>()
                .orderByDesc(UserProviderConfig::getUpdateTime)
                .last("LIMIT 2000"));
        long totalProviderConfigs = providerConfigs.size();
        long enabledProviderConfigs = providerConfigs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getEnabled()))
                .count();
        long readyProviderConfigs = providerConfigs.stream()
                .filter(config -> "ready".equalsIgnoreCase(defaultText(config.getLastCheckStatus(), "")))
                .count();
        long failedProviderConfigs = providerConfigs.stream()
                .filter(config -> "failed".equalsIgnoreCase(defaultText(config.getLastCheckStatus(), "")))
                .count();
        long uncheckedProviderConfigs = providerConfigs.stream()
                .filter(config -> !StringUtils.hasText(config.getLastCheckStatus()))
                .count();
        long configuredProviderUsers = providerConfigs.stream()
                .map(UserProviderConfig::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        List<AiCallLog> aiLogs = aiCallLogMapper.selectList(new LambdaQueryWrapper<AiCallLog>()
                .orderByDesc(AiCallLog::getCreateTime)
                .last("LIMIT 2000"));
        BigDecimal totalEstimatedAiCost = aiLogs.stream()
                .map(AiCallLog::getEstimatedCost)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
        long failedAiCalls = aiLogs.stream()
                .filter(log -> Integer.valueOf(0).equals(log.getSuccess()))
                .count();
        long avgAiLatencyMs = Math.round(aiLogs.stream()
                .map(AiCallLog::getLatencyMs)
                .filter(value -> value != null && value >= 0)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0));

        return AdminRuntimeGovernanceSummaryVO.builder()
                .totalAgentRuns(totalAgentRuns)
                .pendingApprovalRuns(pendingApprovalRuns)
                .rejectedAgentRuns(rejectedAgentRuns)
                .providerBlockedRuns(providerBlockedRuns)
                .totalCopilotPrepSessions(totalCopilotPrepSessions)
                .totalCopilotRealtimeSessions(totalCopilotRealtimeSessions)
                .liveCopilotRealtimeSessions(liveCopilotRealtimeSessions)
                .disconnectedCopilotRealtimeSessions(disconnectedCopilotRealtimeSessions)
                .blockedCopilotRealtimeSessions(blockedCopilotRealtimeSessions)
                .degradedCopilotRealtimeSessions(degradedCopilotRealtimeSessions)
                .totalRecordingReviews(totalRecordingReviews)
                .processingRecordingReviews(processingRecordingReviews)
                .failedRecordingReviews(failedRecordingReviews)
                .readyRecordingReviews(readyRecordingReviews)
                .avgTranscriptTimeMs(avgTranscriptTimeMs)
                .totalProviderConfigs(totalProviderConfigs)
                .enabledProviderConfigs(enabledProviderConfigs)
                .readyProviderConfigs(readyProviderConfigs)
                .failedProviderConfigs(failedProviderConfigs)
                .uncheckedProviderConfigs(uncheckedProviderConfigs)
                .configuredProviderUsers(configuredProviderUsers)
                .totalEstimatedAiCost(totalEstimatedAiCost)
                .failedAiCalls(failedAiCalls)
                .avgAiLatencyMs(avgAiLatencyMs)
                .riskSignals(buildRiskSignals(
                        pendingApprovalRuns,
                        providerBlockedRuns,
                        blockedCopilotRealtimeSessions,
                        disconnectedCopilotRealtimeSessions,
                        processingRecordingReviews,
                        failedRecordingReviews,
                        failedProviderConfigs,
                        uncheckedProviderConfigs,
                        failedAiCalls))
                .recommendations(buildRecommendations(
                        pendingApprovalRuns,
                        blockedCopilotRealtimeSessions,
                        failedRecordingReviews,
                        failedProviderConfigs,
                        uncheckedProviderConfigs,
                        failedAiCalls))
                .build();
    }

    private List<String> buildRiskSignals(long pendingApprovalRuns,
                                          long providerBlockedRuns,
                                          long blockedCopilotRealtimeSessions,
                                          long disconnectedCopilotRealtimeSessions,
                                          long processingRecordingReviews,
                                          long failedRecordingReviews,
                                          long failedProviderConfigs,
                                          long uncheckedProviderConfigs,
                                          long failedAiCalls) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        if (pendingApprovalRuns > 0) {
            signals.add("当前有 " + pendingApprovalRuns + " 个 agent run 等待审批，写操作链路存在堆积。");
        }
        if (providerBlockedRuns > 0 || blockedCopilotRealtimeSessions > 0) {
            signals.add("存在 " + (providerBlockedRuns + blockedCopilotRealtimeSessions) + " 条运行链路被 provider 缺口阻断。");
        }
        if (disconnectedCopilotRealtimeSessions > 0) {
            signals.add("当前有 " + disconnectedCopilotRealtimeSessions + " 条实时 Copilot 会话处于断连状态。");
        }
        if (processingRecordingReviews > 0 || failedRecordingReviews > 0) {
            signals.add("录音转写链路当前有 " + processingRecordingReviews + " 条处理中、" + failedRecordingReviews + " 条失败。");
        }
        if (failedProviderConfigs > 0 || uncheckedProviderConfigs > 0) {
            signals.add("Provider 配置侧存在 " + failedProviderConfigs + " 条探测失败、" + uncheckedProviderConfigs + " 条未完成可用性检查。");
        }
        if (failedAiCalls > 0) {
            signals.add("AI 调用日志里累计有 " + failedAiCalls + " 次失败请求，需要结合场景排查。");
        }
        return new ArrayList<>(signals);
    }

    private List<String> buildRecommendations(long pendingApprovalRuns,
                                              long blockedCopilotRealtimeSessions,
                                              long failedRecordingReviews,
                                              long failedProviderConfigs,
                                              long uncheckedProviderConfigs,
                                              long failedAiCalls) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        if (pendingApprovalRuns > 0) {
            actions.add("优先在 /agent 处理待审批 run，避免学习计划、投递推进和复盘写回继续滞留。");
        }
        if (blockedCopilotRealtimeSessions > 0) {
            actions.add("去设置页补齐 ASR 与联网搜索 provider，减少实时 Copilot 阻断。");
        }
        if (failedRecordingReviews > 0) {
            actions.add("检查录音复盘失败会话的转写网关和存储链路，必要时先重试失败任务。");
        }
        if (failedProviderConfigs > 0 || uncheckedProviderConfigs > 0) {
            actions.add("在 provider 设置区复查失败或未探测配置，优先恢复主模型、ASR 和对象存储可用性。");
        }
        if (failedAiCalls > 0) {
            actions.add("结合 AI 日志中的失败场景与耗时数据，先排查高频失败的模型调用路径。");
        }
        if (actions.isEmpty()) {
            actions.add("当前运行时治理指标整体稳定，可以继续观察成本走势和实时阶段使用量变化。");
        }
        return new ArrayList<>(actions);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
