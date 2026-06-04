package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.TrainingSignalService;
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
import java.util.ArrayList;
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
    private final TrainingSignalService trainingSignalService;

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
    @Transactional(readOnly = true)
    public CopilotRealtimeSessionVO latest(Long userId) {
        CopilotRealtimeSession session = copilotRealtimeSessionMapper.selectOne(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getUserId, userId)
                .orderByDesc(CopilotRealtimeSession::getUpdateTime)
                .orderByDesc(CopilotRealtimeSession::getId)
                .last("LIMIT 1"));
        if (session == null) {
            return null;
        }
        ResumeFile resumeFile = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
        List<CopilotEvent> events = loadEvents(session.getId());
        return buildVo(session, resumeFile == null ? null : resumeFile.getTitle(), events);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO connect(Long userId, Long sessionId) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        if ("blocked".equals(session.getProviderStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "realtime copilot requires ASR and search providers");
        }
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
        trainingSignalService.handleEvidenceUpdate(userId);
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

    @Override
    @Transactional
    public CopilotRealtimeSessionVO appendTranscript(Long userId, Long sessionId, String transcriptText, String speaker) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        String cleanTranscript = trimToNull(transcriptText);
        if (!StringUtils.hasText(cleanTranscript)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "realtime transcript is empty");
        }
        String cleanSpeaker = defaultText(trimToNull(speaker), "候选人");
        appendEvent(
                session,
                "transcript",
                "system",
                cleanSpeaker + "：" + abbreviate(cleanTranscript, 90),
                Map.of("transcriptText", cleanTranscript, "speaker", cleanSpeaker));
        AutoSuggestion autoSuggestion = buildAutoSuggestion(session, cleanTranscript, cleanSpeaker);
        if (autoSuggestion != null) {
            appendEvent(
                    session,
                    "suggestion",
                    "copilot",
                    autoSuggestion.category() + "：" + abbreviate(autoSuggestion.suggestion(), 90),
                    Map.of(
                            "suggestion", autoSuggestion.suggestion(),
                            "category", autoSuggestion.category(),
                            "generated", true,
                            "speaker", cleanSpeaker));
        }
        return detail(userId, sessionId);
    }

    @Override
    @Transactional
    public CopilotRealtimeSessionVO appendSuggestion(Long userId, Long sessionId, String suggestion, String category) {
        CopilotRealtimeSession session = loadRealtimeSession(userId, sessionId);
        String cleanSuggestion = trimToNull(suggestion);
        if (!StringUtils.hasText(cleanSuggestion)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "realtime suggestion is empty");
        }
        String cleanCategory = defaultText(trimToNull(category), "实时提示");
        appendEvent(
                session,
                "suggestion",
                "system",
                cleanCategory + "：" + abbreviate(cleanSuggestion, 90),
                Map.of("suggestion", cleanSuggestion, "category", cleanCategory));
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
                .connectionState(resolveConnectionState(session))
                .canReconnect(resolveCanReconnect(session))
                .providerStatus(session.getProviderStatus())
                .providerStatusMessage(buildProviderStatusMessage(providerReadiness))
                .websocketPath(resolveWebsocketPath(session))
                .prepSummary(session.getPrepSummary())
                .liveChecklist(readStringList(session.getLiveChecklistJson()))
                .providerReadiness(providerReadiness)
                .latestEventSummary(session.getLatestEventSummary())
                .connectedAt(session.getConnectedAt())
                .disconnectedAt(session.getDisconnectedAt())
                .endedAt(session.getEndedAt())
                .postInterviewReview(buildPostInterviewReview(session, eventVos))
                .events(eventVos)
                .updateTime(session.getUpdateTime())
                .build();
    }

    private String resolveConnectionState(CopilotRealtimeSession session) {
        String status = normalize(session.getStatus());
        return switch (status) {
            case "awaiting_connection" -> "ready";
            case "live" -> "connected";
            case "disconnected" -> "disconnected";
            case "completed" -> "closed";
            default -> "unknown";
        };
    }

    private boolean resolveCanReconnect(CopilotRealtimeSession session) {
        String status = normalize(session.getStatus());
        return "awaiting_connection".equals(status) || "disconnected".equals(status);
    }

    private String resolveWebsocketPath(CopilotRealtimeSession session) {
        if (session.getId() == null) {
            return null;
        }
        return "/ws/interview/copilot/" + session.getId();
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

    private CopilotRealtimeSessionVO.PostInterviewReviewVO buildPostInterviewReview(
            CopilotRealtimeSession session,
            List<CopilotRealtimeSessionVO.CopilotRealtimeEventVO> events) {
        if (!"completed".equals(session.getStatus())) {
            return null;
        }

        List<CopilotRealtimeSessionVO.CopilotRealtimeEventVO> orderedEvents = new ArrayList<>(events);
        orderedEvents.sort((left, right) -> {
            LocalDateTime leftTime = left.getCreateTime();
            LocalDateTime rightTime = right.getCreateTime();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return -1;
            }
            if (rightTime == null) {
                return 1;
            }
            return leftTime.compareTo(rightTime);
        });

        List<String> runtimeNotes = orderedEvents.stream()
                .filter(event -> "runtime_note".equals(event.getEventType()))
                .map(CopilotRealtimeSessionVO.CopilotRealtimeEventVO::getSummary)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();
        List<String> transcriptHighlights = orderedEvents.stream()
                .filter(event -> "transcript".equals(event.getEventType()))
                .map(event -> stringPayload(event.getPayload(), "transcriptText"))
                .filter(StringUtils::hasText)
                .map(text -> abbreviate(text, 48))
                .distinct()
                .limit(3)
                .toList();
        List<String> realtimeSuggestions = orderedEvents.stream()
                .filter(event -> "suggestion".equals(event.getEventType()))
                .map(event -> stringPayload(event.getPayload(), "suggestion"))
                .filter(StringUtils::hasText)
                .distinct()
                .limit(3)
                .toList();
        List<String> providerWarnings = readProviderList(session.getProviderReadinessJson()).stream()
                .filter(item -> !"ready".equals(item.getStatus()) && !"saved".equals(item.getStatus()))
                .map(item -> item.getLabel() + " 未完全就绪")
                .limit(2)
                .toList();

        List<String> strengths = new ArrayList<>();
        strengths.add("已完成 Copilot Prep，并把会前重点带入实时阶段。");
        if (session.getConnectedAt() != null) {
            strengths.add("实时连接已建立，说明本轮面试过程至少完成了一次在线跟进。");
        }
        if (!transcriptHighlights.isEmpty()) {
            strengths.add("实时阶段已沉淀 " + transcriptHighlights.size() + " 条转写片段，面后复盘不再只依赖主观回忆。");
        }
        if (!runtimeNotes.isEmpty()) {
            strengths.add("面中已经主动记录关键追问与现场变化，便于面后复盘。");
        }
        if (!realtimeSuggestions.isEmpty()) {
            strengths.add("实时阶段已经输出针对性的提醒和建议，可以直接用于面后复盘。");
        }

        List<String> weakPoints = new ArrayList<>();
        if (providerWarnings.isEmpty() && transcriptHighlights.isEmpty()) {
            weakPoints.add("当前没有自动转写摘要，面后仍需要补一轮结构化复盘，避免现场笔记丢失细节。");
        } else if (!providerWarnings.isEmpty()) {
            weakPoints.add("实时阶段存在依赖降级：" + String.join("、", providerWarnings) + "。");
        }
        if (runtimeNotes.isEmpty()) {
            weakPoints.add("本轮没有记录运行中备注，建议面后补写追问链路和卡壳点。");
        } else {
            weakPoints.add("现场备注已记录，但还没有沉淀成正式训练动作。");
        }
        if (realtimeSuggestions.isEmpty()) {
            weakPoints.add("本轮没有沉淀实时提示，建议面后补写关键追问和更优回答路径。");
        }

        List<String> recommendedActions = new ArrayList<>();
        recommendedActions.add("先把本轮实时阶段转成面后复盘 run，整理追问、卡壳点和表达缺口。");
        if (!runtimeNotes.isEmpty()) {
            recommendedActions.add("优先围绕这些现场备注复盘：" + String.join("；", runtimeNotes) + "。");
        }
        if (!transcriptHighlights.isEmpty()) {
            recommendedActions.add("回放这些实时转写片段，补齐回答结构和例子支撑：" + String.join("；", transcriptHighlights) + "。");
        }
        if (!realtimeSuggestions.isEmpty()) {
            recommendedActions.add("把这些实时提示改写成下一轮训练要求：" + String.join("；", realtimeSuggestions) + "。");
        }
        recommendedActions.add(StringUtils.hasText(session.getJobTitle())
                ? "结合 " + session.getJobTitle() + " 岗位目标，决定是否刷新下一轮训练计划。"
                : "结合当前岗位目标，决定是否刷新下一轮训练计划。");

        String summary = buildPostInterviewReviewSummary(session, runtimeNotes, transcriptHighlights, realtimeSuggestions, providerWarnings);
        return CopilotRealtimeSessionVO.PostInterviewReviewVO.builder()
                .summary(summary)
                .strengths(strengths)
                .weakPoints(weakPoints)
                .recommendedActions(recommendedActions)
                .suggestedAgentType("interview_review")
                .suggestedTriggerSource("interview_live")
                .nextActionLabel("发起面后复盘")
                .nextActionPath("/agent?agentType=interview_review&triggerSource=interview_live&contextRefs=interview:copilot-realtime:" + session.getId() + ",analytics:profile,study-plan:active")
                .build();
    }

    private String buildPostInterviewReviewSummary(
            CopilotRealtimeSession session,
            List<String> runtimeNotes,
            List<String> transcriptHighlights,
            List<String> realtimeSuggestions,
            List<String> providerWarnings) {
        String company = StringUtils.hasText(session.getCompany()) ? session.getCompany() : "当前岗位";
        String role = StringUtils.hasText(session.getJobTitle()) ? session.getJobTitle() : "本轮面试";
        if (!runtimeNotes.isEmpty() || !transcriptHighlights.isEmpty() || !realtimeSuggestions.isEmpty()) {
            return company + " / " + role + " 的实时阶段已结束，已沉淀 "
                    + runtimeNotes.size() + " 条现场备注、"
                    + transcriptHighlights.size() + " 条转写片段和 "
                    + realtimeSuggestions.size() + " 条实时提示，下一步适合直接转入面后复盘。";
        }
        if (!providerWarnings.isEmpty()) {
            return company + " / " + role + " 的实时阶段已结束，但过程中存在依赖降级，建议优先补一轮结构化复盘。";
        }
        return company + " / " + role + " 的实时阶段已结束，下一步建议把现场过程整理成正式复盘和训练动作。";
    }

    private AutoSuggestion buildAutoSuggestion(CopilotRealtimeSession session, String transcriptText, String speaker) {
        if (!StringUtils.hasText(transcriptText)) {
            return null;
        }
        String normalized = transcriptText.trim();
        if (normalized.length() < 8) {
            return null;
        }
        List<String> checklist = readStringList(session.getLiveChecklistJson());
        List<CopilotRealtimeSessionVO.ProviderReadinessVO> providerWarnings = readProviderList(session.getProviderReadinessJson()).stream()
                .filter(item -> !isProviderAvailable(item.getStatus()))
                .toList();

        String category = "自动建议";
        String suggestion;
        if (containsAny(normalized, "不会", "没做过", "不太清楚", "想不起来")) {
            category = "补位提示";
            suggestion = defaultText(speaker, "候选人")
                    + " 这段回答暴露了经验边界，先承认限制，再补一个相近项目、排障动作或取舍案例。";
        } else if (containsAny(normalized, "我们", "项目", "负责", "上线", "优化")) {
            String checklistHint = checklist.isEmpty() ? "把场景、动作、结果和取舍讲完整。" : firstItem(checklist);
            suggestion = "继续围绕项目案例展开，优先量化结果，并记得 " + checklistHint;
        } else if (normalized.length() >= 48) {
            category = "收束提示";
            suggestion = "这段回答已经够长，先收一个结论，再补最关键的技术动作或量化结果，避免继续发散。";
        } else {
            suggestion = "先补结论，再补一条最关键的技术动作或取舍，确保回答能回到岗位价值。";
        }
        if (!providerWarnings.isEmpty()) {
            suggestion += " 当前处于依赖降级，记得手动记下追问链路，避免只依赖自动能力。";
        }
        return new AutoSuggestion(category, suggestion);
    }

    private String resolveProviderStatus(String providerReadinessJson) {
        List<CopilotRealtimeSessionVO.ProviderReadinessVO> providerReadiness = readProviderList(providerReadinessJson);
        boolean requiredMissing = providerReadiness.stream()
                .filter(item -> isRealtimeRequiredScope(item.getScope()))
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        if (requiredMissing) {
            return "blocked";
        }
        boolean degraded = providerReadiness.stream()
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        return degraded ? "degraded" : "ready";
    }

    private String buildProviderStatusMessage(List<CopilotRealtimeSessionVO.ProviderReadinessVO> providerReadiness) {
        List<String> unavailable = providerReadiness.stream()
                .filter(item -> !isProviderAvailable(item.getStatus()))
                .map(CopilotRealtimeSessionVO.ProviderReadinessVO::getLabel)
                .distinct()
                .toList();
        if (unavailable.isEmpty()) {
            return "实时 Copilot 当前依赖已就绪。";
        }
        boolean requiredMissing = providerReadiness.stream()
                .filter(item -> isRealtimeRequiredScope(item.getScope()))
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        return requiredMissing
                ? "实时 Copilot 当前缺少关键依赖：" + String.join("、", unavailable) + "。请先补齐 ASR 和联网搜索配置后再连接实时阶段。"
                : "实时 Copilot 当前有依赖未完全就绪：" + String.join("、", unavailable) + "，实时阶段会按降级模式运行。";
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

    private String stringPayload(Map<String, Object> payload, String key) {
        if (payload == null || !payload.containsKey(key)) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : trimToNull(String.valueOf(value));
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

    private boolean containsAny(String text, String... fragments) {
        String normalized = normalize(text);
        for (String fragment : fragments) {
            if (normalized.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private String firstItem(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "先收束回答";
        }
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                return item.trim();
            }
        }
        return "先收束回答";
    }

    private boolean isRealtimeRequiredScope(String scope) {
        String normalizedScope = normalize(scope);
        return "asr".equals(normalizedScope) || "search".equals(normalizedScope);
    }

    private boolean isProviderAvailable(String status) {
        String normalizedStatus = normalize(status);
        return "ready".equals(normalizedStatus) || "saved".equals(normalizedStatus);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record AutoSuggestion(String category, String suggestion) {
    }
}
