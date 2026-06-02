package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.interview.dto.CopilotRealtimeSessionCreateRequest;
import com.offerpilot.interview.entity.CopilotEvent;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.CopilotRealtimeSession;
import com.offerpilot.interview.mapper.CopilotEventMapper;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.CopilotRealtimeSessionMapper;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewCopilotRealtimeServiceImpl implements InterviewCopilotRealtimeService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<CopilotRealtimeSessionVO.ProviderReadinessVO>> PROVIDER_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CopilotPrepSessionMapper copilotPrepSessionMapper;
    private final CopilotRealtimeSessionMapper copilotRealtimeSessionMapper;
    private final CopilotEventMapper copilotEventMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CopilotRealtimeSessionVO createSession(Long userId, CopilotRealtimeSessionCreateRequest request) {
        CopilotPrepSession prepSession = loadPrepSession(userId, request.getCopilotPrepSessionId());

        CopilotRealtimeSession session = new CopilotRealtimeSession();
        session.setUserId(userId);
        session.setCopilotPrepSessionId(prepSession.getId());
        session.setApplicationId(prepSession.getApplicationId());
        session.setResumeFileId(prepSession.getResumeFileId());
        session.setJobPrepSessionId(prepSession.getJobPrepSessionId());
        session.setCompany(prepSession.getCompany());
        session.setJobTitle(prepSession.getJobTitle());
        session.setStatus("awaiting_connection");
        session.setProviderStatus(resolveProviderStatus(prepSession.getProviderReadinessJson()));
        session.setPrepSummary(prepSession.getSummary());
        session.setLiveChecklistJson(prepSession.getLiveCuesJson());
        session.setProviderReadinessJson(prepSession.getProviderReadinessJson());
        session.setLatestEventSummary("实时阶段已创建，等待建立连接。");
        copilotRealtimeSessionMapper.insert(session);

        appendEvent(
                session,
                "session_created",
                "system",
                "已创建实时 Copilot 会话，等待 WebSocket 连接。",
                Map.of(
                        "copilotPrepSessionId", prepSession.getId(),
                        "providerStatus", session.getProviderStatus()));
        if (StringUtils.hasText(request.getOpeningNote())) {
            appendEvent(
                    session,
                    "opening_note",
                    "client",
                    abbreviate(request.getOpeningNote(), 80),
                    Map.of("note", request.getOpeningNote().trim()));
        }
        return detail(userId, session.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CopilotRealtimeSessionVO detail(Long userId, Long sessionId) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        List<CopilotEvent> events = loadEvents(session.getId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle(), events);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO connect(Long userId, Long sessionId) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        session.setStatus("live");
        session.setConnectedAt(LocalDateTime.now());
        session.setDisconnectedAt(null);
        session.setLatestEventSummary("实时连接已建立，当前会话正在监听中。");
        copilotRealtimeSessionMapper.updateById(session);

        appendEvent(
                session,
                "connection_established",
                "system",
                "实时 Copilot 已连接，可以接收后续转写和提示事件。",
                Map.of("status", session.getStatus(), "providerStatus", session.getProviderStatus()));
        if ("degraded".equals(session.getProviderStatus())) {
            appendEvent(
                    session,
                    "provider_degraded",
                    "system",
                    "部分 provider 未完全就绪，当前实时阶段会按降级模式运行。",
                    Map.of("providerStatus", session.getProviderStatus()));
        }
        return detail(userId, sessionId);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO disconnect(Long userId, Long sessionId, String reason) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        if ("completed".equals(session.getStatus())) {
            return detail(userId, sessionId);
        }
        session.setStatus("disconnected");
        session.setDisconnectedAt(LocalDateTime.now());
        session.setLatestEventSummary(defaultText(reason, "实时连接已断开。"));
        copilotRealtimeSessionMapper.updateById(session);
        appendEvent(
                session,
                "connection_closed",
                "system",
                defaultText(reason, "实时连接已断开。"),
                Map.of("status", session.getStatus()));
        return detail(userId, sessionId);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO complete(Long userId, Long sessionId, String summary) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        session.setStatus("completed");
        session.setEndedAt(LocalDateTime.now());
        session.setLatestEventSummary(defaultText(trimToNull(summary), "实时阶段已手动结束，准备进入面后复盘。"));
        copilotRealtimeSessionMapper.updateById(session);
        appendEvent(
                session,
                "session_completed",
                "client",
                session.getLatestEventSummary(),
                Map.of("status", session.getStatus()));
        return detail(userId, sessionId);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO appendClientNote(Long userId, Long sessionId, String note) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        String cleanNote = trimToNull(note);
        if (!StringUtils.hasText(cleanNote)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "realtime note is empty");
        }
        session.setLatestEventSummary(abbreviate(cleanNote, 80));
        copilotRealtimeSessionMapper.updateById(session);
        appendEvent(session, "runtime_note", "client", abbreviate(cleanNote, 80), Map.of("note", cleanNote));
        return detail(userId, sessionId);
    }

    private CopilotPrepSession loadPrepSession(Long userId, Long sessionId) {
        CopilotPrepSession session = copilotPrepSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "copilot prep session not found");
        }
        return session;
    }

    private CopilotRealtimeSession loadRealtimeSession(Long userId, Long sessionId) {
        CopilotRealtimeSession session = copilotRealtimeSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "copilot realtime session not found");
        }
        return session;
    }

    private List<CopilotEvent> loadEvents(Long sessionId) {
        return copilotEventMapper.selectList(new LambdaQueryWrapper<CopilotEvent>()
                .eq(CopilotEvent::getSessionId, sessionId)
                .orderByDesc(CopilotEvent::getCreateTime)
                .last("LIMIT 20"));
    }

    private CopilotRealtimeSessionVO buildVo(CopilotRealtimeSession session, String resumeTitle, List<CopilotEvent> events) {
        List<CopilotRealtimeSessionVO.CopilotRealtimeEventVO> eventVos = events.stream()
                .map(this::buildEventVo)
                .toList();
        List<CopilotRealtimeSessionVO.ProviderReadinessVO> providerReadiness = readProviderList(session.getProviderReadinessJson());
        return CopilotRealtimeSessionVO.builder()
                .id(session.getId())
                .copilotPrepSessionId(session.getCopilotPrepSessionId())
                .applicationId(session.getApplicationId())
                .resumeFileId(session.getResumeFileId())
                .jobPrepSessionId(session.getJobPrepSessionId())
                .resumeTitle(resumeTitle)
                .company(session.getCompany())
                .jobTitle(session.getJobTitle())
                .status(session.getStatus())
                .providerStatus(session.getProviderStatus())
                .prepSummary(session.getPrepSummary())
                .liveChecklist(readStringList(session.getLiveChecklistJson()))
                .providerReadiness(providerReadiness)
                .latestEventSummary(session.getLatestEventSummary())
                .connectedAt(session.getConnectedAt())
                .disconnectedAt(session.getDisconnectedAt())
                .endedAt(session.getEndedAt())
                .events(eventVos)
                .updateTime(session.getUpdateTime())
                .build();
    }

    private CopilotRealtimeSessionVO.CopilotRealtimeEventVO buildEventVo(CopilotEvent event) {
        return CopilotRealtimeSessionVO.CopilotRealtimeEventVO.builder()
                .id(event.getId())
                .sessionId(event.getSessionId())
                .eventType(event.getEventType())
                .source(event.getSource())
                .summary(event.getSummary())
                .payload(readPayloadMap(event.getPayloadJson()))
                .createTime(event.getCreateTime())
                .build();
    }

    private void appendEvent(CopilotRealtimeSession session, String eventType, String source, String summary, Map<String, Object> payload) {
        CopilotEvent event = new CopilotEvent();
        event.setSessionId(session.getId());
        event.setUserId(session.getUserId());
        event.setEventType(eventType);
        event.setSource(source);
        event.setSummary(summary);
        event.setPayloadJson(writePayload(payload));
        copilotEventMapper.insert(event);
        session.setLatestEventSummary(summary);
        copilotRealtimeSessionMapper.updateById(session);
    }

    private String resolveProviderStatus(String providerReadinessJson) {
        boolean degraded = readProviderList(providerReadinessJson).stream()
                .anyMatch(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus()));
        return degraded ? "degraded" : "ready";
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse realtime checklist json", e);
            return List.of();
        }
    }

    private List<CopilotRealtimeSessionVO.ProviderReadinessVO> readProviderList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, PROVIDER_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse realtime provider json", e);
            return List.of();
        }
    }

    private Map<String, Object> readPayloadMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse realtime event payload", e);
            return Map.of();
        }
    }

    private String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "failed to serialize realtime payload");
        }
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String clean = text.trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
