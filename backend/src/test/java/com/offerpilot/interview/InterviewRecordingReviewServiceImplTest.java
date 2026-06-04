package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.RecordingTranscriptSegment;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.RecordingTranscriptSegmentMapper;
import com.offerpilot.interview.service.impl.InterviewRecordingReviewAsyncProcessor;
import com.offerpilot.interview.service.impl.InterviewRecordingReviewServiceImpl;
import com.offerpilot.interview.voice.SttGateway;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewRecordingReviewServiceImplTest {

    @Mock
    private RecordingReviewSessionMapper recordingReviewSessionMapper;
    @Mock
    private RecordingTranscriptSegmentMapper recordingTranscriptSegmentMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private SttGateway sttGateway;
    @Mock
    private InterviewRecordingReviewAsyncProcessor recordingReviewAsyncProcessor;
    @Mock
    private UserProviderConfigService userProviderConfigService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterviewRecordingReviewServiceImpl service;

    @Test
    void createReview_includesProviderReadinessWhenOssMissing() {
        when(sttGateway.isAvailable()).thenReturn(true);
        when(fileStorageService.store(any(), any(), any(), any())).thenReturn(StoredFile.builder()
                .storageKey("interview/audio/demo.webm")
                .contentType("audio/webm")
                .size(128L)
                .build());
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of(
                UserProviderConfigItemVO.builder()
                        .scope("asr")
                        .label("语音识别")
                        .status("ready")
                        .statusMessage("配置完整，可供对应能力使用。")
                        .build()));
        when(recordingReviewSessionMapper.insert(any(RecordingReviewSession.class))).thenAnswer(invocation -> {
            RecordingReviewSession session = invocation.getArgument(0);
            session.setId(88L);
            return 1;
        });

        RecordingReviewSessionVO result = service.createReview(
                1L,
                "Java",
                "后端开发",
                "重点看表达结构",
                null,
                new byte[] {1, 2, 3},
                "audio/webm",
                "demo.webm");

        assertEquals("audio", result.getInputMode());
        assertEquals("processing", result.getStatus());
        assertEquals(2, result.getProviderReadiness().size());
        assertEquals("asr", result.getProviderReadiness().get(0).getScope());
        assertEquals("ready", result.getProviderReadiness().get(0).getStatus());
        assertEquals("oss", result.getProviderReadiness().get(1).getScope());
        assertEquals("missing", result.getProviderReadiness().get(1).getStatus());
        assertEquals("degraded", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("对象存储"));
        assertEquals("recording_review", result.getSuggestedAgentType());
        assertEquals("recording_review", result.getSuggestedTriggerSource());
        assertEquals("转成训练动作", result.getNextActionLabel());
        assertTrue(result.getNextActionPath().contains("interview:recording-review:88"));
        assertTrue(result.getProviderReadiness().get(1).getStatusMessage().contains("长音频上传能力可能受限"));
        verify(recordingReviewAsyncProcessor).processReview(eq(88L), any(byte[].class), eq("audio/webm"));
    }

    @Test
    void createReview_allowsTranscriptModeWhenAsrMissing() {
        AtomicReference<RecordingReviewSession> insertedSession = new AtomicReference<>();
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of(
                UserProviderConfigItemVO.builder()
                        .scope("asr")
                        .label("语音识别")
                        .status("missing")
                        .statusMessage("还没有保存这类配置。")
                        .build(),
                UserProviderConfigItemVO.builder()
                        .scope("oss")
                        .label("对象存储")
                        .status("ready")
                        .statusMessage("配置完整，可供对应能力使用。")
                        .build()));
        when(recordingReviewSessionMapper.insert(any(RecordingReviewSession.class))).thenAnswer(invocation -> {
            RecordingReviewSession session = invocation.getArgument(0);
            session.setId(99L);
            insertedSession.set(session);
            return 1;
        });
        doAnswer(invocation -> {
            RecordingReviewSession session = insertedSession.get();
            session.setStatus("ready");
            session.setStatusMessage("已基于文字 transcript 生成复盘结果。");
            session.setTranscript("首先我会先给出结论，然后补一个项目例子。");
            session.setSummary("这段「后端开发」录音复盘分约 68 分，已有一定结构，也有实战线索。");
            return null;
        }).when(recordingReviewAsyncProcessor).processTranscriptReview(99L, "首先我会先给出结论，然后补一个项目例子。");
        when(recordingReviewSessionMapper.selectById(99L)).thenAnswer(invocation -> insertedSession.get());
        when(recordingTranscriptSegmentMapper.selectList(any())).thenReturn(List.of(
                createSegment(9901L, 1, "首先我会先给出结论，然后补一个项目例子")));

        RecordingReviewSessionVO result = service.createReview(
                1L,
                "Java",
                "后端开发",
                "项目追问",
                "首先我会先给出结论，然后补一个项目例子。",
                null,
                null,
                null);

        assertEquals("transcript", result.getInputMode());
        assertEquals("ready", result.getStatus());
        assertEquals("degraded", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("文字 transcript 模式仍可继续"));
        assertTrue(result.getTranscript().contains("首先我会先给出结论"));
        assertTrue(result.getSummary().contains("后端开发"));
        assertEquals(1, result.getSegments().size());
        verify(recordingReviewAsyncProcessor).processTranscriptReview(99L, "首先我会先给出结论，然后补一个项目例子。");
        verify(recordingReviewAsyncProcessor, never()).processReview(eq(99L), any(byte[].class), any());
    }

    @Test
    void latest_returnsMostRecentRecordingReview() {
        RecordingReviewSession session = new RecordingReviewSession();
        session.setId(33L);
        session.setUserId(1L);
        session.setDirection("Java 后端");
        session.setJobRole("后端开发");
        session.setAudioUrl("interview/audio/33.webm");
        session.setStatus("completed");
        session.setSummary("最近一次录音复盘");
        session.setOverallScore(new BigDecimal("67"));
        session.setWeakPointsJson("[\"表达结构\"]");
        session.setSuggestedActionsJson("[\"先回听薄弱片段\"]");
        session.setUpdateTime(LocalDateTime.of(2026, 6, 3, 9, 30));
        when(recordingReviewSessionMapper.selectOne(any())).thenReturn(session);
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of());
        when(recordingTranscriptSegmentMapper.selectList(any())).thenReturn(List.of(
                createSegment(701L, 0, "这里是片段转写")));

        RecordingReviewSessionVO result = service.latest(1L);

        assertEquals("33", String.valueOf(result.getId()));
        assertEquals("audio", result.getInputMode());
        assertEquals("Java 后端", result.getDirection());
        assertEquals("blocked", result.getProviderStatus());
        assertTrue(result.getProviderStatusMessage().contains("关键依赖"));
        assertEquals("recording_review", result.getSuggestedAgentType());
        assertTrue(result.getNextActionPath().contains("interview:recording-review:33"));
        assertEquals(1, result.getSegments().size());
        assertEquals("这里是片段转写", result.getSegments().get(0).getTranscriptText());
    }

    private RecordingTranscriptSegment createSegment(Long id, Integer index, String text) {
        RecordingTranscriptSegment segment = new RecordingTranscriptSegment();
        segment.setId(id);
        segment.setSessionId(33L);
        segment.setSegmentIndex(index);
        segment.setTranscriptText(text);
        return segment;
    }
}
