package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.RecordingTranscriptSegment;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.RecordingTranscriptSegmentMapper;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.voice.SttGateway;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewRecordingReviewServiceImpl implements InterviewRecordingReviewService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final List<String> RECORDING_REVIEW_PROVIDER_SCOPES = List.of("asr", "oss");

    private final RecordingReviewSessionMapper recordingReviewSessionMapper;
    private final RecordingTranscriptSegmentMapper recordingTranscriptSegmentMapper;
    private final FileStorageService fileStorageService;
    private final SttGateway sttGateway;
    private final InterviewRecordingReviewAsyncProcessor recordingReviewAsyncProcessor;
    private final UserProviderConfigService userProviderConfigService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RecordingReviewSessionVO createReview(Long userId, String direction, String jobRole, String notes,
                                                 byte[] audioData, String mimeType, String originalFilename) {
        if (!sttGateway.isAvailable()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "录音复盘功能未启用，请先配置 ASR 服务");
        }

        StoredFile storedFile = fileStorageService.store(
                StorageDirectory.INTERVIEW_AUDIO,
                originalFilename,
                audioData,
                mimeType);

        RecordingReviewSession session = new RecordingReviewSession();
        session.setUserId(userId);
        session.setDirection(trimToNull(direction));
        session.setJobRole(trimToNull(jobRole));
        session.setNotes(trimToNull(notes));
        session.setAudioUrl(storedFile.getStorageKey());
        session.setStatus("processing");
        session.setStatusMessage("录音已上传，正在排队转写。");
        session.setSummary("录音已上传，正在排队转写。");
        recordingReviewSessionMapper.insert(session);

        launchAfterCommit(session.getId(), audioData, mimeType);
        return buildVo(session, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingReviewSessionVO detail(Long userId, Long sessionId) {
        RecordingReviewSession session = recordingReviewSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "recording review session not found");
        }
        return buildVo(session, loadSegments(sessionId));
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingReviewSessionVO latest(Long userId) {
        RecordingReviewSession session = recordingReviewSessionMapper.selectOne(new LambdaQueryWrapper<RecordingReviewSession>()
                .eq(RecordingReviewSession::getUserId, userId)
                .orderByDesc(RecordingReviewSession::getUpdateTime)
                .orderByDesc(RecordingReviewSession::getId)
                .last("LIMIT 1"));
        if (session == null) {
            return null;
        }
        return buildVo(session, loadSegments(session.getId()));
    }

    private void launchAfterCommit(Long sessionId, byte[] audioData, String mimeType) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            recordingReviewAsyncProcessor.processReview(sessionId, audioData, mimeType);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                recordingReviewAsyncProcessor.processReview(sessionId, audioData, mimeType);
            }
        });
    }

    private List<RecordingTranscriptSegment> loadSegments(Long sessionId) {
        return recordingTranscriptSegmentMapper.selectList(new LambdaQueryWrapper<RecordingTranscriptSegment>()
                .eq(RecordingTranscriptSegment::getSessionId, sessionId)
                .orderByAsc(RecordingTranscriptSegment::getSegmentIndex)
                .orderByAsc(RecordingTranscriptSegment::getId));
    }

    private RecordingReviewSessionVO buildVo(RecordingReviewSession session, List<RecordingTranscriptSegment> segments) {
        return RecordingReviewSessionVO.builder()
                .id(session.getId())
                .direction(session.getDirection())
                .jobRole(session.getJobRole())
                .notes(session.getNotes())
                .status(session.getStatus())
                .statusMessage(session.getStatusMessage())
                .transcript(session.getTranscript())
                .transcriptConfidence(session.getTranscriptConfidence())
                .transcriptTimeMs(session.getTranscriptTimeMs())
                .overallScore(session.getOverallScore())
                .summary(session.getSummary())
                .strengths(readList(session.getStrengthsJson()))
                .weakPoints(readList(session.getWeakPointsJson()))
                .suggestedActions(readList(session.getSuggestedActionsJson()))
                .providerReadiness(resolveProviderReadiness())
                .segments(segments.stream()
                        .map(item -> RecordingReviewSessionVO.SegmentVO.builder()
                                .id(item.getId())
                                .segmentIndex(item.getSegmentIndex())
                                .transcriptText(item.getTranscriptText())
                                .startOffsetMs(item.getStartOffsetMs())
                                .endOffsetMs(item.getEndOffsetMs())
                                .signalType(item.getSignalType())
                                .build())
                        .toList())
                .updateTime(session.getUpdateTime())
                .build();
    }

    private List<RecordingReviewSessionVO.ProviderReadinessVO> resolveProviderReadiness() {
        Map<String, UserProviderConfigItemVO> configMap = new LinkedHashMap<>();
        for (UserProviderConfigItemVO item : userProviderConfigService.listCurrentUserConfigs()) {
            if (item != null && StringUtils.hasText(item.getScope())) {
                configMap.put(item.getScope(), item);
            }
        }
        List<RecordingReviewSessionVO.ProviderReadinessVO> readiness = new ArrayList<>();
        for (String scope : RECORDING_REVIEW_PROVIDER_SCOPES) {
            UserProviderConfigItemVO item = configMap.get(scope);
            String label = item == null ? scope.toUpperCase(Locale.ROOT) : item.getLabel();
            String status = item == null ? "missing" : item.getStatus();
            String statusMessage = item == null ? "还没有保存这类配置。" : item.getStatusMessage();
            if ("oss".equals(scope) && item == null) {
                statusMessage = "还没有保存对象存储配置，长音频上传能力可能受限。";
            }
            readiness.add(RecordingReviewSessionVO.ProviderReadinessVO.builder()
                    .scope(scope)
                    .label(label)
                    .status(status)
                    .statusMessage(statusMessage)
                    .build());
        }
        return readiness;
    }

    private List<String> readList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read recording review list", e);
            return List.of();
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
