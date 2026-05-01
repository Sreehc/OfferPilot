package com.bytecoach.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.service.AiOrchestratorService;
import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.entity.ChatMessage;
import com.bytecoach.chat.entity.ChatSession;
import com.bytecoach.chat.mapper.ChatMessageMapper;
import com.bytecoach.chat.mapper.ChatSessionMapper;
import com.bytecoach.chat.service.impl.ChatServiceImpl;
import com.bytecoach.chat.vo.ChatMessageVO;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.chat.vo.ChatSessionVO;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private AiOrchestratorService aiOrchestratorService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void send_createsNewSession() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(null);
        request.setMessage("What is Spring AOP?");
        request.setMode("rag");

        ChatSendVO aiResult = ChatSendVO.builder()
                .answer("Spring AOP is...")
                .references(List.of())
                .build();
        when(aiOrchestratorService.answerChat(any())).thenReturn(aiResult);
        when(chatSessionMapper.insert(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession s = invocation.getArgument(0);
            s.setId(100L);
            return 1;
        });
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSendVO result = chatService.send(1L, request);

        assertNotNull(result);
        assertEquals(100L, result.getSessionId());
        verify(chatSessionMapper).insert(any(ChatSession.class));
        verify(chatMessageMapper, times(2)).insert(any(ChatMessage.class));
    }

    @Test
    void send_usesExistingSession() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(50L);
        request.setMessage("Follow-up question");
        request.setMode("rag");

        ChatSession existing = new ChatSession();
        existing.setId(50L);
        existing.setUserId(1L);
        existing.setTitle("Existing Session");
        existing.setMode("rag");
        when(chatSessionMapper.selectById(50L)).thenReturn(existing);

        ChatSendVO aiResult = ChatSendVO.builder().answer("Answer").references(List.of()).build();
        when(aiOrchestratorService.answerChat(any())).thenReturn(aiResult);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSendVO result = chatService.send(1L, request);

        assertEquals(50L, result.getSessionId());
        verify(chatSessionMapper, never()).insert(any());
    }

    @Test
    void send_wrongOwner_throws() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(50L);
        request.setMessage("test");
        request.setMode("chat");

        ChatSession existing = new ChatSession();
        existing.setId(50L);
        existing.setUserId(999L);
        when(chatSessionMapper.selectById(50L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> chatService.send(1L, request));
    }

    @Test
    void listSessions_returnsPageResult() {
        ChatSession s1 = new ChatSession();
        s1.setId(1L);
        s1.setUserId(1L);
        s1.setTitle("Session 1");
        s1.setMode("rag");
        s1.setLastMessageTime(LocalDateTime.now());

        when(chatSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(chatSessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(s1));

        PageResult<ChatSessionVO> result = chatService.listSessions(1L, 1, 20);

        assertEquals(1, result.getRecords().size());
        assertEquals("Session 1", result.getRecords().get(0).getTitle());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void listMessages_ownershipCheck() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        when(chatSessionMapper.selectById(10L)).thenReturn(session);

        ChatMessage msg = new ChatMessage();
        msg.setId(1L);
        msg.setSessionId(10L);
        msg.setRole("user");
        msg.setMessageType("text");
        msg.setContent("Hello");
        when(chatMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(msg));

        List<ChatMessageVO> result = chatService.listMessages(1L, 10L);

        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getRole());
    }

    @Test
    void deleteSession_ownershipCheck() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        when(chatSessionMapper.selectById(10L)).thenReturn(session);

        chatService.deleteSession(1L, 10L);

        verify(chatMessageMapper).delete(any(LambdaQueryWrapper.class));
        verify(chatSessionMapper).deleteById(10L);
    }
}
