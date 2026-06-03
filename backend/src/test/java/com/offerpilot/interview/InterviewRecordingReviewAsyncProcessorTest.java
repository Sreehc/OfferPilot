package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.interview.entity.RecordingReviewSession;
import com.offerpilot.interview.entity.RecordingTranscriptSegment;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.interview.mapper.RecordingTranscriptSegmentMapper;
import com.offerpilot.interview.service.impl.InterviewRecordingReviewAsyncProcessor;
import com.offerpilot.interview.support.RecordingReviewBlueprintFactory;
import com.offerpilot.interview.voice.SttGateway;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewRecordingReviewAsyncProcessorTest {

    @Mock
    private RecordingReviewSessionMapper recordingReviewSessionMapper;
    @Mock
    private RecordingTranscriptSegmentMapper recordingTranscriptSegmentMapper;
    @Mock
    private RecordingReviewBlueprintFactory blueprintFactory;
    @Mock
    private SttGateway sttGateway;
    @Mock
    private TrainingSignalService trainingSignalService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InterviewRecordingReviewAsyncProcessor processor;

    @Test
    void processReview_readySessionRefreshesAbilityProfile() throws Exception {
        RecordingReviewSession session = new RecordingReviewSession();
        session.setId(21L);
        session.setUserId(9L);
        session.setDirection("Java后端");
        session.setJobRole("Spring工程师");
        session.setNotes("重点看表达结构");
        session.setStatus("queued");
        session.setCreateTime(LocalDateTime.now().minusMinutes(3));

        RecordingReviewBlueprintFactory.RecordingReviewBlueprint blueprint =
                RecordingReviewBlueprintFactory.RecordingReviewBlueprint.builder()
                        .overallScore(new BigDecimal("78"))
                        .summary("录音复盘已生成")
                        .strengths(List.of("结构较清晰"))
                        .weakPoints(List.of("项目案例不够"))
                        .suggestedActions(List.of("补 1 个项目例子"))
                        .segments(List.of(
                                RecordingReviewBlueprintFactory.SegmentBlueprint.builder()
                                        .segmentIndex(1)
                                        .transcriptText("首先我会说明结论")
                                        .startOffsetMs(0)
                                        .endOffsetMs(15000)
                                        .signalType("structure")
                                        .build()))
                        .build();

        when(recordingReviewSessionMapper.selectById(21L)).thenReturn(session);
        when(sttGateway.transcribe(any(), any())).thenReturn(new SttGateway.SttResult(
                "首先我会说明结论，然后补一个项目例子。",
                new BigDecimal("0.91"),
                3200));
        when(blueprintFactory.build(any(), any(), any(), any())).thenReturn(blueprint);
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"ok\"]");

        processor.processReview(21L, new byte[] {1, 2, 3}, "audio/webm");

        verify(recordingReviewSessionMapper, atLeast(3)).updateById(any(RecordingReviewSession.class));
        verify(recordingTranscriptSegmentMapper).insert(any(RecordingTranscriptSegment.class));
        verify(trainingSignalService).handleEvidenceUpdate(9L);
        assertEquals("ready", session.getStatus());
        assertEquals("录音复盘已生成。", session.getStatusMessage());
        assertEquals(new BigDecimal("78"), session.getOverallScore());
    }
}
