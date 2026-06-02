package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        when(copilotRealtimeSessionMapper.selectById(45L)).thenAnswer(invocation -> storedSession);
        doAnswer(invocation -> {
            CopilotRealtimeSession updated = invocation.getArgument(0);
            storedSession = updated;
            if (storedSession.getUpdateTime() == null) {
                storedSession.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 30));
            }
            return 1;
        }).when(copilotRealtimeSessionMapper).updateById(any(CopilotRealtimeSession.class));
        doAnswer(invocation -> {
            CopilotEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(700L + storedEvents.size());
            }
            event.setCreateTime(LocalDateTime.of(2026, 6, 2, 10, 5 + storedEvents.size()));
            storedEvents.add(event);
            return 1;
        }).when(copilotEventMapper).insert(any(CopilotEvent.class));
        when(copilotEventMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedEvents));

        ResumeFile resumeFile = new ResumeFile();
        resumeFile.setId(9L);
        resumeFile.setTitle("Java 后端简历");
        when(resumeFileMapper.selectById(9L)).thenReturn(resumeFile);
    }

    @Test
    void complete_buildsStructuredPostInterviewReview() {
        CopilotRealtimeSessionVO noteSnapshot = service.appendClientNote(1L, 45L, "面试官追问 Redis 双写一致性，项目例子没讲稳。");
        assertEquals("live", noteSnapshot.getStatus());

        CopilotRealtimeSessionVO completed = service.complete(1L, 45L, "当前实时阶段已结束，转入面后复盘。");

        assertEquals("completed", completed.getStatus());
        assertNotNull(completed.getPostInterviewReview());
        assertTrue(completed.getPostInterviewReview().getSummary().contains("实时阶段已结束"));
        assertTrue(completed.getPostInterviewReview().getWeakPoints().stream().anyMatch(item -> item.contains("依赖降级")));
        assertTrue(completed.getPostInterviewReview().getRecommendedActions().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertEquals("interview_review", completed.getPostInterviewReview().getSuggestedAgentType());
        assertTrue(completed.getPostInterviewReview().getNextActionPath().contains("interview:copilot-realtime:45"));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "runtime_note".equals(item.getEventType())));
        assertTrue(completed.getEvents().stream().anyMatch(item -> "session_completed".equals(item.getEventType())));
    }
}
