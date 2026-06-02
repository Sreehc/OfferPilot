package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.RecordingTranscriptSegmentMapper;
import com.offerpilot.interview.service.impl.InterviewRecordingReviewAsyncProcessor;
import com.offerpilot.interview.service.impl.InterviewRecordingReviewServiceImpl;
import com.offerpilot.interview.voice.SttGateway;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import java.util.List;
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
                new byte[] {1, 2, 3},
                "audio/webm",
                "demo.webm");

        assertEquals("processing", result.getStatus());
        assertEquals(2, result.getProviderReadiness().size());
        assertEquals("asr", result.getProviderReadiness().get(0).getScope());
        assertEquals("ready", result.getProviderReadiness().get(0).getStatus());
        assertEquals("oss", result.getProviderReadiness().get(1).getScope());
        assertEquals("missing", result.getProviderReadiness().get(1).getStatus());
        assertTrue(result.getProviderReadiness().get(1).getStatusMessage().contains("长音频上传能力可能受限"));
        verify(recordingReviewAsyncProcessor).processReview(eq(88L), any(byte[].class), eq("audio/webm"));
    }
}
