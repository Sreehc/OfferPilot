package com.offerpilot.interview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.interview.websocket.CopilotRealtimeWebSocketHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class CopilotRealtimeWebSocketHandlerTest {

    @Mock
    private InterviewCopilotRealtimeService interviewCopilotRealtimeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CopilotRealtimeWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CopilotRealtimeWebSocketHandler(interviewCopilotRealtimeService, objectMapper);
    }

    @Test
    void noteEvent_broadcastsSnapshotToAllSessionsInSameRealtimeSession() throws Exception {
        CopilotRealtimeSessionVO connectedSnapshot = realtimeSnapshot("live", "实时连接已建立。");
        CopilotRealtimeSessionVO updatedSnapshot = realtimeSnapshot("live", "运行中备注已更新。");
        when(interviewCopilotRealtimeService.connect(eq(1L), eq(45L))).thenReturn(connectedSnapshot);
        when(interviewCopilotRealtimeService.appendClientNote(eq(1L), eq(45L), eq("先稳住项目背景。")))
                .thenReturn(updatedSnapshot);

        List<String> sessionOneMessages = new ArrayList<>();
        List<String> sessionTwoMessages = new ArrayList<>();
        WebSocketSession sessionOne = mockSession("ws-1", 1L, 45L, sessionOneMessages);
        WebSocketSession sessionTwo = mockSession("ws-2", 1L, 45L, sessionTwoMessages);

        handler.afterConnectionEstablished(sessionOne);
        handler.afterConnectionEstablished(sessionTwo);
        handler.handleMessage(sessionOne, new TextMessage("{\"type\":\"note\",\"note\":\"先稳住项目背景。\"}"));

        assertEquals(3, sessionOneMessages.size());
        assertEquals(2, sessionTwoMessages.size());
        assertSnapshotSummary(sessionOneMessages.get(2), "运行中备注已更新。");
        assertSnapshotSummary(sessionTwoMessages.get(1), "运行中备注已更新。");
        verify(interviewCopilotRealtimeService).appendClientNote(1L, 45L, "先稳住项目背景。");
    }

    @Test
    void closingLastSession_disconnectsRealtimeSession() throws Exception {
        when(interviewCopilotRealtimeService.connect(anyLong(), eq(45L))).thenReturn(realtimeSnapshot("live", "实时连接已建立。"));

        WebSocketSession session = mockSession("ws-1", 1L, 45L, new ArrayList<>());

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(interviewCopilotRealtimeService).disconnect(1L, 45L, "实时连接已断开，可以稍后重新连接。");
    }

    private WebSocketSession mockSession(String sessionId, Long userId, long realtimeSessionId, List<String> messages) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("userId", userId);
        when(session.getId()).thenReturn(sessionId);
        when(session.getUri()).thenReturn(new URI("ws://localhost/ws/interview/copilot/" + realtimeSessionId));
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        doAnswer(invocation -> {
            TextMessage message = invocation.getArgument(0);
            messages.add(message.getPayload());
            return null;
        }).when(session).sendMessage(org.mockito.ArgumentMatchers.any(TextMessage.class));
        return session;
    }

    private CopilotRealtimeSessionVO realtimeSnapshot(String status, String latestEventSummary) {
        return CopilotRealtimeSessionVO.builder()
                .id(45L)
                .status(status)
                .latestEventSummary(latestEventSummary)
                .events(List.of())
                .providerReadiness(List.of())
                .liveChecklist(List.of())
                .build();
    }

    private void assertSnapshotSummary(String payload, String expectedSummary) throws Exception {
        JsonNode json = objectMapper.readTree(payload);
        assertEquals("snapshot", json.path("type").asText());
        assertEquals(expectedSummary, json.path("session").path("latestEventSummary").asText());
        assertTrue(json.hasNonNull("serverTime"));
    }
}
