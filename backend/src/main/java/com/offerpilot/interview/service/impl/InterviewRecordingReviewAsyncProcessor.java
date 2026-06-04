package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.RecordingTranscriptSegment;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.RecordingTranscriptSegmentMapper;
import com.offerpilot.interview.support.RecordingReviewBlueprintFactory;
import com.offerpilot.interview.support.RecordingReviewBlueprintFactory.RecordingReviewBlueprint;
import com.offerpilot.interview.support.RecordingReviewBlueprintFactory.SegmentBlueprint;
import com.offerpilot.interview.voice.SttGateway;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewRecordingReviewAsyncProcessor {

    private final RecordingReviewSessionMapper recordingReviewSessionMapper;
    private final RecordingTranscriptSegmentMapper recordingTranscriptSegmentMapper;
    private final RecordingReviewBlueprintFactory blueprintFactory;
    private final SttGateway sttGateway;
    private final TrainingSignalService trainingSignalService;
    private final ObjectMapper objectMapper;

    @Async
    @Transactional
    public void processReview(Long sessionId, byte[] audioData, String mimeType) {
        RecordingReviewSession session = recordingReviewSessionMapper.selectById(sessionId);
        if (session == null) {
            log.warn("Recording review session {} missing before async processing", sessionId);
            return;
        }

        try {
            updateStatus(session, "transcribing", "正在转写录音。");
            SttGateway.SttResult sttResult = sttGateway.transcribe(audioData, mimeType);
            String transcript = trimToNull(sttResult.text());
            if (!StringUtils.hasText(transcript)) {
                fail(session, "转写结果为空，请重新上传更清晰的录音。");
                return;
            }

            completeReviewFromTranscript(
                    session,
                    transcript,
                    sttResult.confidence(),
                    sttResult.processingTimeMs(),
                    "已拿到转写结果，正在整理复盘建议。",
                    "录音复盘已生成。");
        } catch (Exception e) {
            log.error("Async recording review processing failed for session {}", sessionId, e);
            fail(session, "录音复盘生成失败: " + abbreviate(e.getMessage(), 120));
        }
    }

    @Transactional
    public void processTranscriptReview(Long sessionId, String transcript) {
        RecordingReviewSession session = recordingReviewSessionMapper.selectById(sessionId);
        if (session == null) {
            log.warn("Recording review session {} missing before transcript processing", sessionId);
            return;
        }
        try {
            completeReviewFromTranscript(
                    session,
                    transcript,
                    null,
                    null,
                    "已收到文字 transcript，正在整理复盘建议。",
                    "已基于文字 transcript 生成复盘结果。");
        } catch (Exception e) {
            log.error("Transcript recording review processing failed for session {}", sessionId, e);
            fail(session, "文字 transcript 复盘生成失败: " + abbreviate(e.getMessage(), 120));
        }
    }

    private void completeReviewFromTranscript(RecordingReviewSession session,
                                              String transcript,
                                              BigDecimal confidence,
                                              Integer processingTimeMs,
                                              String analyzingMessage,
                                              String readyMessage) {
        session.setTranscript(transcript);
        session.setTranscriptConfidence(confidence);
        session.setTranscriptTimeMs(processingTimeMs);
        session.setStatus("analyzing");
        session.setStatusMessage(analyzingMessage);
        session.setSummary(analyzingMessage);
        recordingReviewSessionMapper.updateById(session);

        RecordingReviewBlueprint blueprint = blueprintFactory.build(
                transcript,
                session.getDirection(),
                session.getJobRole(),
                session.getNotes());

        recordingTranscriptSegmentMapper.delete(new LambdaQueryWrapper<RecordingTranscriptSegment>()
                .eq(RecordingTranscriptSegment::getSessionId, session.getId()));
        persistSegments(session, blueprint.segments());

        session.setStatus("ready");
        session.setStatusMessage(readyMessage);
        session.setOverallScore(blueprint.overallScore());
        session.setSummary(blueprint.summary());
        session.setStrengthsJson(writeList(blueprint.strengths()));
        session.setWeakPointsJson(writeList(blueprint.weakPoints()));
        session.setSuggestedActionsJson(writeList(blueprint.suggestedActions()));
        recordingReviewSessionMapper.updateById(session);
        trainingSignalService.handleEvidenceUpdate(session.getUserId());
    }

    private void persistSegments(RecordingReviewSession session, List<SegmentBlueprint> segments) {
        for (SegmentBlueprint segment : segments) {
            RecordingTranscriptSegment entity = new RecordingTranscriptSegment();
            entity.setSessionId(session.getId());
            entity.setUserId(session.getUserId());
            entity.setSegmentIndex(segment.segmentIndex());
            entity.setTranscriptText(segment.transcriptText());
            entity.setStartOffsetMs(segment.startOffsetMs());
            entity.setEndOffsetMs(segment.endOffsetMs());
            entity.setSignalType(segment.signalType());
            recordingTranscriptSegmentMapper.insert(entity);
        }
    }

    private void updateStatus(RecordingReviewSession session, String status, String message) {
        session.setStatus(status);
        session.setStatusMessage(message);
        session.setSummary(message);
        recordingReviewSessionMapper.updateById(session);
    }

    private void fail(RecordingReviewSession session, String message) {
        session.setStatus("failed");
        session.setStatusMessage(message);
        session.setSummary(message);
        recordingReviewSessionMapper.updateById(session);
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write recording review list", e);
            return "[]";
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String abbreviate(String value, int limit) {
        if (!StringUtils.hasText(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 1)) + "…";
    }
}
