package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.interview.entity.CopilotEvent;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.CopilotRealtimeSession;
import com.offerpilot.interview.mapper.CopilotEventMapper;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.CopilotRealtimeSessionMapper;
import com.offerpilot.interview.service.impl.InterviewCopilotRealtimeServiceImpl;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewCopilotRealtimeServiceImplTest {

    @Mock
    private CopilotPrepSessionMapper copilotPrepSessionMapper;
    @Mock
    private CopilotRealtimeSessionMapper copilotRealtimeSessionMapper;
    @Mock
    private CopilotEventMapper copilotEventMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private TrainingSignalService trainingSignalService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private InterviewCopilotRealtimeServiceImpl service;

    private CopilotRealtimeSession storedSession;
    private final List<CopilotEvent> storedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        storedSession = new CopilotRealtimeSession();
        storedSession.setId(45L);
        storedSession.setUserId(1L);
        storedSession.setCopilotPrepSessionId(11L);
        storedSession.setResumeFileId(9L);
        storedSession.setCompany("字节跳动");
        storedSession.setJobTitle("Java 后端开发");
        storedSession.setStatus("live");
        storedSession.setProviderStatus("degraded");
        storedSession.setPrepSummary("先准备缓存一致性和项目亮点。");
        storedSession.setLiveChecklistJson("[\"先讲项目背景\",\"遇到追问先收束再展开\"]");
        storedSession.setProviderReadinessJson("""
                [
                  {"scope":"asr","label":"语音识别","status":"ready","statusMessage":"配置完整"},
                  {"scope":"search","label":"联网搜索","status":"missing","statusMessage":"还没有保存这类配置。"}
                ]
                """);
        storedSession.setConnectedAt(LocalDateTime.of(2026, 6, 2, 10, 0));
        storedSession.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 0));

        lenient().when(copilotRealtimeSessionMapper.selectById(45L)).thenAnswer(invocation -> storedSession);
        lenient().doAnswer(invocation -> {
            CopilotRealtimeSession updated = invocation.getArgument(0);
            storedSession = updated;
            if (storedSession.getUpdateTime() == null) {
                storedSession.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30));
            }
            return 1;
        }).when(copilotRealtimeSessionMapper).updateById(any(CopilotRealtimeSession.class));
        lenient().doAnswer(invocation -> {
            CopilotEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(700L + storedEvents.size());
            }
            event.setCreateTime(LocalDateTime.of(2026, 6, 2, 10, 5 + storedEvents.size()));
            storedEvents.add(event);
            return 1;
        }).when(copilotEventMapper).insert(any(CopilotEvent.class));
        lenient().when(copilotEventMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedEvents));

        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setId(9L);
        resumeFile.setTitle("Java 后端简历");
        lenient().when(resumeFileMapper.selectById(9L)).thenReturn(resumeFile);
    }

    @Test
    void complete_buildsStructuredPostInterviewReview() {
        CopilotRealtimeSessionVO noteSnapshot = service.appendClientNote(1L, 45L, "面试官追问 Redis 双写一致性，项目例子没讲稳。");
        assertEquals("live", noteSnapshot.getStatus());
        assertEquals("connected", noteSnapshot.getConnectionState());
        assertEquals(Boolean.FALSE, noteSnapshot.getCanReconnect());
        assertEquals("/ws/interview/copilot/45", noteSnapshot.getWebsocketPath());
        CopilotRealtimeSessionVO transcriptSnapshot = service.appendTranscript(1L, 45L, "我先讲项目背景，再讲缓存一致性和补偿策略。", "候选人");
        assertTrue(transcriptSnapshot.getEvents().stream().anyMatch(item -> "transcript".equals(item.getEventType())));
        assertTrue(transcriptSnapshot.getEvents().stream().anyMatch(item ->
                "suggestion".equals(item.getEventType())
                        && "copilot".equals(item.getSource())
                        && String.valueOf(item.getPayload().get("generated")).contains("true")));
        CopilotRealtimeSessionVO suggestionSnapshot = service.appendSuggestion(1L, 45L, "先收束回答，再补一个 Redis 双写修复案例。", "追问提示");
        assertTrue(suggestionSnapshot.getEvents().stream().anyMatch(item -> "suggestion".equals(item.getEventType())));

        CopilotRealtimeSessionVO completed = service.complete(1L, 45L, "当前实时阶段已结束，转入面后复盘。");

        assertEquals("completed", completed.getStatus());
        assertEquals("closed", completed.getConnectionState());
        assertEquals(Boolean.FALSE, completed.getCanReconnect());
        assertEquals("/ws/interview/copilot/45", completed.getWebsocketPath());
        assertNotNull(completed.getPostInterviewReview());
        assertTrue(completed.getPostInterviewReview().getSummary().contains("实时阶段已结束"));
        assertTrue(completed.getPostInterviewReview().getSummary().contains("转写片段"));
        assertTrue(completed.getPostInterviewReview().getWeakPoints().stream().anyMatch(item -> item.contains("依赖降级")));
        assertTrue(completed.getPostInterviewReview().getRecommendedActions().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(completed.getPostInterviewReview().getRecommendedActions().stream().anyMatch(item -> item.contains("Redis 双写修复案例")));
        assertEquals("interview_review", completed.getPostInterviewReview().getSuggestedAgentType());
        assertTrue(completed.getPostInterviewReview().getNextActionPath().contains("interview:copilot-realtime:45"));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "runtime_note".equals(item.getEventType())));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "transcript".equals(item.getEventType())));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "suggestion".equals(item.getEventType())));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "session_completed".equals(item.getEventType())));
        verify(trainingSignalService).handleEvidenceUpdate(1L);
    }

    @Test
    void appendTranscript_generatesAutoSuggestionForWeakSignal() {
        CopilotRealtimeSessionVO snapshot = service.appendTranscript(1L, 45L, "这个点我没做过，暂时想不起来具体案例。", "候选人");

        assertTrue(snapshot.getEvents().stream().anyMatch(item ->
                "suggestion".equals(item.getEventType())
                        && "copilot".equals(item.getSource())
                        && String.valueOf(item.getPayload().get("suggestion")).contains("先承认限制")));
    }

    @Test
    void latest_returnsMostRecentSessionSnapshot() {
        storedSession.setId(88L);
        storedSession.setStatus("completed");
        storedSession.setEndedAt(LocalDateTime.of(2026, 6, 2, 11, 30));
        storedSession.setLatestEventSummary("当前实时阶段已结束，准备进入面后复盘。");
        storedEvents.add(realtimeEvent(801L, "runtime_note", "client", "先稳住项目背景，再回答缓存一致性。", 12));
        storedEvents.add(realtimeEvent(802L, "session_completed", "client", "当前实时阶段已结束，准备进入面后复盘。", 13));

        when(copilotRealtimeSessionMapper.selectOne(any())).thenReturn(storedSession);

        CopilotRealtimeSessionVO latest = service.latest(1L);

        assertNotNull(latest);
        assertEquals(88L, latest.getId());
        assertEquals("Java 后端简历", latest.getResumeTitle());
        assertEquals("completed", latest.getStatus());
        assertEquals("closed", latest.getConnectionState());
        assertEquals(Boolean.FALSE, latest.getCanReconnect());
        assertEquals("/ws/interview/copilot/88", latest.getWebsocketPath());
        assertEquals("实时 Copilot 当前缺少关键依赖：联网搜索。请先补齐 ASR 和联网搜索配置后再连接实时阶段。", latest.getProviderStatusMessage());
        assertNotNull(latest.getPostInterviewReview());
        assertTrue(latest.getPostInterviewReview().getRecommendedActions().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(latest.getEvents().stream().anyMatch(item -> "runtime_note".equals(item.getEventType())));
        verify(copilotRealtimeSessionMapper).selectOne(any());
    }

    @Test
    void detail_exposesReconnectContractForDisconnectedSession() {
        storedSession.setStatus("disconnected");
        storedSession.setDisconnectedAt(LocalDateTime.of(2026, 6, 2, 10, 20));
        storedSession.setLatestEventSummary("实时连接已断开，可以稍后重新连接。");

        CopilotRealtimeSessionVO detail = service.detail(1L, 45L);

        assertEquals("disconnected", detail.getStatus());
        assertEquals("disconnected", detail.getConnectionState());
        assertEquals(Boolean.TRUE, detail.getCanReconnect());
        assertEquals("/ws/interview/copilot/45", detail.getWebsocketPath());
        assertEquals("实时 Copilot 当前缺少关键依赖：联网搜索。请先补齐 ASR 和联网搜索配置后再连接实时阶段。", detail.getProviderStatusMessage());
    }

    @Test
    void connect_rejectsBlockedRealtimeSession() {
        storedSession.setStatus("awaiting_connection");
        storedSession.setProviderStatus("blocked");

        BusinessException exception = assertThrows(BusinessException.class, () -> service.connect(1L, 45L));

        assertEquals("realtime copilot requires ASR and search providers", exception.getMessage());
    }

    private CopilotEvent realtimeEvent(Long id, String eventType, String source, String summary, int minute) {
        CopilotEvent event = new CopilotEvent();
        event.setId(id);
        event.setSessionId(storedSession.getId());
        event.setUserId(storedSession.getUserId());
        event.setEventType(eventType);
        event.setSource(source);
        event.setSummary(summary);
        event.setCreateTime(LocalDateTime.of(2026, 6, 2, 10, minute));
        return event;
    }
}
