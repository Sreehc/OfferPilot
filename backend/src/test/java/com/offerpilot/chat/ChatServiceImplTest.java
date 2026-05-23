package com.offerpilot.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.entity.ChatMessage;
import com.offerpilot.chat.entity.ChatSession;
import com.offerpilot.chat.mapper.ChatMessageMapper;
import com.offerpilot.chat.mapper.ChatSessionMapper;
import com.offerpilot.chat.service.impl.ChatServiceImpl;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;
    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private AiOrchestratorService aiOrchestratorService;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private ResumeProjectMapper resumeProjectMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() throws Exception {
        Field selfField = ChatServiceImpl.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(chatService, chatService);
    }

    @Test
    void send_createsNewSession() {
        ChatSendRequest request = new ChatSendRequest();
        request.setMessage("What is Spring AOP?");
        request.setMode("chat");
        request.setAnswerMode("learning");

        ChatSendVO aiResult = ChatSendVO.builder()
                .answer("Spring AOP is...")
                .references(List.of())
                .build();
        when(aiOrchestratorService.answerChat(any())).thenReturn(aiResult);
        when(chatSessionMapper.insert(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            session.setId(100L);
            return 1;
        });
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSendVO result = chatService.send(1L, request);

        assertNotNull(result);
        assertEquals(100L, result.getSessionId());
        verify(chatSessionMapper).insert(any(ChatSession.class));
        verify(chatMessageMapper, org.mockito.Mockito.times(2)).insert(any(ChatMessage.class));
    }

    @Test
    void send_usesExistingSession() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(50L);
        request.setMessage("Follow-up question");
        request.setMode("chat");
        request.setAnswerMode("learning");

        ChatSession existing = new ChatSession();
        existing.setId(50L);
        existing.setUserId(1L);
        existing.setTitle("Existing Session");
        existing.setMode("chat");
        when(chatSessionMapper.selectById(50L)).thenReturn(existing);

        ChatSendVO aiResult = ChatSendVO.builder().answer("Answer").references(List.of()).build();
        when(aiOrchestratorService.answerChat(any())).thenReturn(aiResult);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        ChatSendVO result = chatService.send(1L, request);

        assertEquals(50L, result.getSessionId());
        verify(chatSessionMapper, never()).insert(any(ChatSession.class));
    }

    @Test
    void send_wrongOwner_throws() {
        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(50L);
        request.setMessage("test");
        request.setMode("chat");
        request.setAnswerMode("learning");

        ChatSession existing = new ChatSession();
        existing.setId(50L);
        existing.setUserId(999L);
        when(chatSessionMapper.selectById(50L)).thenReturn(existing);

        assertThrows(BusinessException.class, () -> chatService.send(1L, request));
    }

    @Test
    void listSessions_returnsPageResult() {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setUserId(1L);
        session.setTitle("Session 1");
        session.setMode("rag");
        session.setLastMessageTime(LocalDateTime.now());

        when(chatSessionMapper.selectCount(any())).thenReturn(1L);
        when(chatSessionMapper.selectList(any())).thenReturn(List.of(session));

        PageResult<ChatSessionVO> result = chatService.listSessions(1L, 1, 20);

        assertEquals(1, result.getRecords().size());
        assertEquals("Session 1", result.getRecords().get(0).getTitle());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void listMessages_checksOwnership() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        when(chatSessionMapper.selectById(10L)).thenReturn(session);

        ChatMessage message = new ChatMessage();
        message.setId(1L);
        message.setSessionId(10L);
        message.setRole("user");
        message.setMessageType("text");
        message.setContent("Hello");
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(message));

        List<ChatMessageVO> result = chatService.listMessages(1L, 10L);

        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getRole());
    }

    @Test
    void deleteSession_checksOwnership() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        when(chatSessionMapper.selectById(10L)).thenReturn(session);

        chatService.deleteSession(1L, 10L);

        verify(chatMessageMapper).delete(any());
        verify(chatSessionMapper).deleteById(10L);
    }
}
