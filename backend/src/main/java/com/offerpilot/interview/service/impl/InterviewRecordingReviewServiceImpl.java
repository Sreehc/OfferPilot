package com.offerpilot.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewRecordingReviewServiceImpl implements InterviewRecordingReviewService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final RecordingReviewSessionMapper recordingReviewSessionMapper;
    private final RecordingTranscriptSegmentMapper recordingTranscriptSegmentMapper;
    private final FileStorageService fileStorageService;
    private final SttGateway sttGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RecordingReviewSessionVO createReview(Long userId, String direction, String jobRole, String notes,
                                                 byte[] audioData, String mimeType, String originalFilename) {
        if (!sttGateway.isAvailable()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "录音复盘功能未启用，请先配置 ASR 服务");
        }

        SttGateway.SttResult sttResult;
        try {
            sttResult = sttGateway.transcribe(audioData, mimeType);
        } catch (Exception e) {
            log.error("Recording review transcription failed", e);
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "录音转写失败: " + e.getMessage());
        }

        String transcript = sttResult.text();
        if (!StringUtils.hasText(transcript)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "录音转写结果为空，请重新上传");
        }

        StoredFile storedFile = fileStorageService.store(
                StorageDirectory.INTERVIEW_AUDIO,
                originalFilename,
                audioData,
                mimeType);

        ReviewBlueprint blueprint = buildBlueprint(transcript, direction, jobRole, notes);

        RecordingReviewSession session = new RecordingReviewSession();
        session.setUserId(userId);
        session.setDirection(trimToNull(direction));
        session.setJobRole(trimToNull(jobRole));
        session.setNotes(trimToNull(notes));
        session.setAudioUrl(storedFile.getStorageKey());
        session.setStatus("ready");
        session.setTranscript(transcript);
        session.setTranscriptConfidence(sttResult.confidence());
        session.setTranscriptTimeMs(sttResult.processingTimeMs());
        session.setOverallScore(blueprint.overallScore());
        session.setSummary(blueprint.summary());
        session.setStrengthsJson(writeList(blueprint.strengths()));
        session.setWeakPointsJson(writeList(blueprint.weakPoints()));
        session.setSuggestedActionsJson(writeList(blueprint.suggestedActions()));
        recordingReviewSessionMapper.insert(session);

        for (SegmentBlueprint segment : blueprint.segments()) {
            RecordingTranscriptSegment entity = new RecordingTranscriptSegment();
            entity.setSessionId(session.getId());
            entity.setUserId(userId);
            entity.setSegmentIndex(segment.segmentIndex());
            entity.setTranscriptText(segment.transcriptText());
            entity.setStartOffsetMs(segment.startOffsetMs());
            entity.setEndOffsetMs(segment.endOffsetMs());
            entity.setSignalType(segment.signalType());
            recordingTranscriptSegmentMapper.insert(entity);
        }

        return buildVo(session, loadSegments(session.getId()));
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

    private List<RecordingTranscriptSegment> loadSegments(Long sessionId) {
        return recordingTranscriptSegmentMapper.selectList(new LambdaQueryWrapper<RecordingTranscriptSegment>()
                .eq(RecordingTranscriptSegment::getSessionId, sessionId)
                .orderByAsc(RecordingTranscriptSegment::getSegmentIndex)
                .orderByAsc(RecordingTranscriptSegment::getId));
    }

    private ReviewBlueprint buildBlueprint(String transcript, String direction, String jobRole, String notes) {
        TranscriptMetrics metrics = measureTranscript(transcript);
        BigDecimal overallScore = scoreTranscript(metrics);
        List<SegmentBlueprint> segments = splitSegments(transcript, metrics.estimatedDurationMs());
        List<String> strengths = buildStrengths(metrics, direction);
        List<String> weakPoints = buildWeakPoints(metrics, direction, jobRole);
        List<String> suggestedActions = buildSuggestedActions(metrics, weakPoints, notes);
        String summary = buildSummary(metrics, overallScore, direction, jobRole);
        return new ReviewBlueprint(overallScore, summary, strengths, weakPoints, suggestedActions, segments);
    }

    private TranscriptMetrics measureTranscript(String transcript) {
        String normalized = transcript.replaceAll("\\s+", " ").trim();
        int charCount = normalized.length();
        int sentenceCount = (int) Arrays.stream(normalized.split("[。！？!?；;\\n]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .count();
        int structureHits = countContains(normalized, List.of("首先", "然后", "最后", "第一", "第二", "因为", "所以", "总结"));
        int exampleHits = countContains(normalized, List.of("例如", "比如", "举个例子", "实际", "项目", "上线", "排查", "优化"));
        int fillerHits = countContains(normalized.toLowerCase(Locale.ROOT), List.of("嗯", "额", "然后呢", "这个", "那个", "就是"));
        int estimatedDurationMs = Math.max(60000, charCount * 320);
        return new TranscriptMetrics(charCount, sentenceCount, structureHits, exampleHits, fillerHits, estimatedDurationMs);
    }

    private BigDecimal scoreTranscript(TranscriptMetrics metrics) {
        double score = 55;
        if (metrics.charCount() >= 180) {
            score += 10;
        }
        if (metrics.charCount() >= 320) {
            score += 8;
        }
        score += Math.min(metrics.structureHits(), 4) * 4.5;
        score += Math.min(metrics.exampleHits(), 4) * 4.0;
        score -= Math.min(metrics.fillerHits(), 6) * 2.5;
        if (metrics.sentenceCount() <= 2) {
            score -= 8;
        }
        score = Math.max(35, Math.min(95, score));
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private List<SegmentBlueprint> splitSegments(String transcript, int estimatedDurationMs) {
        List<String> sentences = Arrays.stream(transcript.split("[。！？!?；;\\n]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (sentences.isEmpty()) {
            return List.of(SegmentBlueprint.builder()
                    .segmentIndex(1)
                    .transcriptText(transcript.trim())
                    .startOffsetMs(0)
                    .endOffsetMs(estimatedDurationMs)
                    .signalType("general")
                    .build());
        }
        int segmentDuration = Math.max(12000, estimatedDurationMs / sentences.size());
        List<SegmentBlueprint> segments = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String text = sentences.get(i);
            segments.add(SegmentBlueprint.builder()
                    .segmentIndex(i + 1)
                    .transcriptText(text)
                    .startOffsetMs(i * segmentDuration)
                    .endOffsetMs((i + 1) * segmentDuration)
                    .signalType(resolveSignalType(text))
                    .build());
        }
        return segments;
    }

    private String resolveSignalType(String text) {
        if (containsAny(text, List.of("例如", "比如", "项目", "上线", "排查", "优化"))) {
            return "example";
        }
        if (containsAny(text, List.of("首先", "然后", "最后", "总结", "第一", "第二"))) {
            return "structure";
        }
        if (containsAny(text, List.of("因为", "所以", "原因", "取舍", "权衡"))) {
            return "reasoning";
        }
        return "general";
    }

    private List<String> buildStrengths(TranscriptMetrics metrics, String direction) {
        LinkedHashSet<String> strengths = new LinkedHashSet<>();
        if (metrics.structureHits() >= 2) {
            strengths.add("回答里有明显的结构化组织，适合继续收紧成“结论 -> 原因 -> 例子”。");
        }
        if (metrics.exampleHits() >= 2) {
            strengths.add("已经出现项目或实战案例线索，说明这段录音不是纯背概念。");
        }
        if (metrics.charCount() >= 260) {
            strengths.add("表达长度足够支撑一轮完整回答，素材量不是当前主要问题。");
        }
        if (strengths.isEmpty()) {
            strengths.add("这段录音已经提供了可复盘的基础素材，下一步重点是把答案结构再压紧。");
        }
        if (StringUtils.hasText(direction)) {
            strengths.add("后续复盘可以继续围绕「" + direction.trim() + "」主题做专项收口。");
        }
        return strengths.stream().limit(4).toList();
    }

    private List<String> buildWeakPoints(TranscriptMetrics metrics, String direction, String jobRole) {
        LinkedHashSet<String> weakPoints = new LinkedHashSet<>();
        if (metrics.fillerHits() >= 3) {
            weakPoints.add("口头填充词偏多，容易稀释重点，建议先压缩“嗯/这个/就是”这类停顿语。");
        }
        if (metrics.structureHits() == 0) {
            weakPoints.add("缺少明确的答题骨架，面试官会更难快速抓住你的结论。");
        }
        if (metrics.exampleHits() == 0) {
            weakPoints.add("当前回答更偏概念层，没有明显项目案例或工程细节支撑。");
        }
        if (metrics.charCount() < 140) {
            weakPoints.add("回答偏短，可能还没把背景、原因和结果解释清楚。");
        }
        if (StringUtils.hasText(jobRole)) {
            weakPoints.add("需要再对齐「" + jobRole.trim() + "」岗位视角，把表达拉回业务结果、稳定性或性能价值。");
        } else if (StringUtils.hasText(direction)) {
            weakPoints.add("建议继续围绕「" + direction.trim() + "」主题补充典型追问和工程取舍。");
        }
        return weakPoints.stream().limit(4).toList();
    }

    private List<String> buildSuggestedActions(TranscriptMetrics metrics, List<String> weakPoints, String notes) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        if (metrics.structureHits() == 0) {
            actions.add("先把这道题改写成 3 句骨架：先结论，再原因，最后补一个项目例子。");
        }
        if (metrics.exampleHits() == 0) {
            actions.add("补 1 个可量化项目案例，明确说出场景、动作、结果和取舍。");
        }
        if (metrics.fillerHits() >= 3) {
            actions.add("重新录一遍时先放慢节奏，刻意减少口头填充词。");
        }
        if (StringUtils.hasText(notes)) {
            actions.add("结合这次备注“" + abbreviate(notes.trim(), 18) + "”再回听录音，确认是否真的回答到了场景重点。");
        }
        actions.add("下一轮可以把这段录音里的薄弱点带入模拟面试或错题复盘。");
        return actions.stream().limit(4).toList();
    }

    private String buildSummary(TranscriptMetrics metrics, BigDecimal overallScore, String direction, String jobRole) {
        String topic = StringUtils.hasText(jobRole) ? jobRole.trim()
                : StringUtils.hasText(direction) ? direction.trim() : "本轮面试";
        String structure = metrics.structureHits() > 0 ? "已有一定结构" : "结构还不够稳定";
        String example = metrics.exampleHits() > 0 ? "也有实战线索" : "但缺少项目案例支撑";
        return "这段「" + topic + "」录音复盘分约 "
                + overallScore.stripTrailingZeros().toPlainString()
                + " 分，"
                + structure
                + "，"
                + example
                + "。";
    }

    private RecordingReviewSessionVO buildVo(RecordingReviewSession session, List<RecordingTranscriptSegment> segments) {
        return RecordingReviewSessionVO.builder()
                .id(session.getId())
                .direction(session.getDirection())
                .jobRole(session.getJobRole())
                .notes(session.getNotes())
                .status(session.getStatus())
                .transcript(session.getTranscript())
                .transcriptConfidence(session.getTranscriptConfidence())
                .transcriptTimeMs(session.getTranscriptTimeMs())
                .overallScore(session.getOverallScore())
                .summary(session.getSummary())
                .strengths(readList(session.getStrengthsJson()))
                .weakPoints(readList(session.getWeakPointsJson()))
                .suggestedActions(readList(session.getSuggestedActionsJson()))
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

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write recording review list", e);
            return "[]";
        }
    }

    private int countContains(String text, List<String> tokens) {
        int count = 0;
        for (String token : tokens) {
            if (text.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private boolean containsAny(String text, List<String> tokens) {
        return tokens.stream().anyMatch(text::contains);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String abbreviate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private record TranscriptMetrics(int charCount, int sentenceCount, int structureHits, int exampleHits,
                                     int fillerHits, int estimatedDurationMs) {
    }

    private record ReviewBlueprint(BigDecimal overallScore, String summary, List<String> strengths,
                                   List<String> weakPoints, List<String> suggestedActions,
                                   List<SegmentBlueprint> segments) {
    }

    @lombok.Builder
    private record SegmentBlueprint(int segmentIndex, String transcriptText, int startOffsetMs,
                                    int endOffsetMs, String signalType) {
    }
}
