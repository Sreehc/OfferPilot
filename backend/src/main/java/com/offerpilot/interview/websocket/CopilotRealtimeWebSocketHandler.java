package com.offerpilot.interview.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class CopilotRealtimeWebSocketHandler extends TextWebSocketHandler {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final InterviewCopilotRealtimeService interviewCopilotRealtimeService;
    private final ObjectMapper objectMapper;
    private final Map<Long, Map<String, WebSocketSession>> activeConnections = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = attributeAsLong(session, "userId");
        Long realtimeSessionId = extractSessionId(session.getUri());
        session.getAttributes().put("realtimeSessionId", realtimeSessionId);
        activeConnections.computeIfAbsent(realtimeSessionId, ignored -> new ConcurrentHashMap<>()).put(session.getId(), session);
        CopilotRealtimeSessionVO snapshot = interviewCopilotRealtimeService.connect(userId, realtimeSessionId);
        broadcastSnapshot(realtimeSessionId, snapshot);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = attributeAsLong(session, "userId");
        Long realtimeSessionId = attributeAsLong(session, "realtimeSessionId");
        Map<String, Object> payload = parsePayload(message.getPayload());
        String type = String.valueOf(payload.getOrDefault("type", "ping"));

        if ("ping".equals(type)) {
            sendEnvelope(session, "pong", Map.of("serverTime", Instant.now().toString()));
            return;
        }
        if ("note".equals(type)) {
            String note = String.valueOf(payload.getOrDefault("note", "")).trim();
            CopilotRealtimeSessionVO snapshot = interviewCopilotRealtimeService.appendClientNote(userId, realtimeSessionId, note);
            broadcastSnapshot(realtimeSessionId, snapshot);
            return;
        }
        if ("transcript".equals(type)) {
            String transcript = String.valueOf(payload.getOrDefault("transcript", "")).trim();
            String speaker = String.valueOf(payload.getOrDefault("speaker", "")).trim();
            CopilotRealtimeSessionVO snapshot = interviewCopilotRealtimeService.appendTranscript(userId, realtimeSessionId, transcript, speaker);
            broadcastSnapshot(realtimeSessionId, snapshot);
            return;
        }
        if ("suggestion".equals(type)) {
            String suggestion = String.valueOf(payload.getOrDefault("suggestion", "")).trim();
            String category = String.valueOf(payload.getOrDefault("category", "")).trim();
            CopilotRealtimeSessionVO snapshot = interviewCopilotRealtimeService.appendSuggestion(userId, realtimeSessionId, suggestion, category);
            broadcastSnapshot(realtimeSessionId, snapshot);
            return;
        }
        if ("complete".equals(type)) {
            String summary = String.valueOf(payload.getOrDefault("summary", "")).trim();
            CopilotRealtimeSessionVO snapshot = interviewCopilotRealtimeService.complete(userId, realtimeSessionId, summary);
            broadcastSnapshot(realtimeSessionId, snapshot);
            return;
        }

        sendEnvelope(session, "error", Map.of("message", "unsupported realtime message type"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long realtimeSessionId = attributeAsLong(session, "realtimeSessionId");
        Long userId = attributeAsLong(session, "userId");
        Map<String, WebSocketSession> sessions = activeConnections.get(realtimeSessionId);
        if (sessions != null) {
            sessions.remove(session.getId());
            if (sessions.isEmpty()) {
                activeConnections.remove(realtimeSessionId);
                interviewCopilotRealtimeService.disconnect(userId, realtimeSessionId, "实时连接已断开，可以稍后重新连接。");
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("copilot_websocket_transport_error sessionId={} error={}",
                session.getAttributes().get("realtimeSessionId"),
                exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private Map<String, Object> parsePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, MAP_TYPE);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("type", "note");
            fallback.put("note", payload);
            return fallback;
        }
    }

    private void sendEnvelope(WebSocketSession session, String type, Map<String, Object> payload) throws IOException {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", type);
        envelope.putAll(payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
    }

    private void broadcastSnapshot(Long realtimeSessionId, CopilotRealtimeSessionVO snapshot) {
        Map<String, WebSocketSession> sessions = activeConnections.get(realtimeSessionId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String serverTime = Instant.now().toString();
        List<String> staleSessionIds = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession targetSession = entry.getValue();
            if (targetSession == null || !targetSession.isOpen()) {
                staleSessionIds.add(entry.getKey());
                continue;
            }
            try {
                sendEnvelope(targetSession, "snapshot", Map.of("session", snapshot, "serverTime", serverTime));
            } catch (IOException e) {
                log.warn("Failed to broadcast realtime snapshot sessionId={} websocketSessionId={} error={}",
                        realtimeSessionId, entry.getKey(), e.getMessage());
                staleSessionIds.add(entry.getKey());
            }
        }
        staleSessionIds.forEach(sessions::remove);
        if (sessions.isEmpty()) {
            activeConnections.remove(realtimeSessionId);
        }
    }

    private Long extractSessionId(URI uri) {
        if (uri == null || !StringUtils.hasText(uri.getPath())) {
            throw new IllegalArgumentException("missing realtime session path");
        }
        String[] parts = uri.getPath().split("/");
        String last = parts[parts.length - 1];
        return Long.valueOf(last);
    }

    private Long attributeAsLong(WebSocketSession session, String key) {
        Object value = session.getAttributes().get(key);
        if (value == null) {
            throw new IllegalStateException("missing websocket attribute: " + key);
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        return Long.valueOf(String.valueOf(value));
    }
}
