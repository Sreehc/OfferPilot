package com.offerpilot.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import org.mockito.ArgumentCaptor;
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
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.common.storage.UploadPolicyService;
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
import org.springframework.mock.web.MockMultipartFile;

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
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private UploadPolicyService uploadPolicyService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() throws Exception {
        Field selfField = ChatServiceImpl.class.getDeclaredField("self");
        selfField.setAccessible(true);
        selfField.set(chatService, chatService);
        lenient().when(chatMessageMapper.selectOne(any())).thenReturn(null);
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
    void send_duplicateClientMessageId_returnsExistingAssistantWithoutCallingAiAgain() {
        ChatSendRequest request = new ChatSendRequest();
        request.setMessage("What is Spring AOP?");
        request.setMode("chat");
        request.setAnswerMode("learning");
        request.setClientMessageId("client-msg-1");

        ChatSession session = new ChatSession();
        session.setId(100L);
        session.setUserId(1L);
        session.setTitle("Spring AOP");
        session.setMode("chat");
        when(chatSessionMapper.selectById(100L)).thenReturn(session);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setId(200L);
        userMessage.setSessionId(100L);
        userMessage.setUserId(1L);
        userMessage.setRole("user");
        userMessage.setContent("What is Spring AOP?");
        userMessage.setClientMessageId("client-msg-1");

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setId(201L);
        assistantMessage.setSessionId(100L);
        assistantMessage.setUserId(1L);
        assistantMessage.setRole("assistant");
        assistantMessage.setMessageType("text");
        assistantMessage.setContent("Existing answer");
        assistantMessage.setClientMessageId("client-msg-1");

        when(chatMessageMapper.selectOne(any())).thenReturn(userMessage, assistantMessage);

        ChatSendVO result = chatService.send(1L, request);

        assertEquals(100L, result.getSessionId());
        assertEquals("Spring AOP", result.getSessionTitle());
        assertEquals("Existing answer", result.getAnswer());
        verify(aiOrchestratorService, never()).answerChat(any());
        verify(chatMessageMapper, never()).insert(any(ChatMessage.class));
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

    @Test
    void renameSession_trimsTitleAndChecksOwnership() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        session.setTitle("Old title");
        when(chatSessionMapper.selectById(10L)).thenReturn(session);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);

        ChatSessionVO result = chatService.renameSession(1L, 10L, "  New interview title  ");

        assertEquals("New interview title", result.getTitle());
        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(captor.capture());
        assertEquals("New interview title", captor.getValue().getTitle());
    }

    @Test
    void renameSession_blankTitleThrows() {
        assertThrows(BusinessException.class, () -> chatService.renameSession(1L, 10L, "   "));
        verify(chatSessionMapper, never()).updateById(any(ChatSession.class));
    }

    @Test
    void regenerateMessage_appendsNewAssistantAnswerWithoutDuplicatingUserMessage() {
        ChatSession session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        session.setTitle("Existing Session");
        session.setMode("chat");

        ChatMessage assistant = new ChatMessage();
        assistant.setId(20L);
        assistant.setSessionId(10L);
        assistant.setUserId(1L);
        assistant.setRole("assistant");
        assistant.setContent("Old answer");

        ChatMessage user = new ChatMessage();
        user.setId(19L);
        user.setSessionId(10L);
        user.setUserId(1L);
        user.setRole("user");
        user.setContent("Explain HashMap resize");

        when(chatMessageMapper.selectById(20L)).thenReturn(assistant);
        when(chatSessionMapper.selectById(10L)).thenReturn(session);
        when(chatMessageMapper.selectOne(any())).thenReturn(user);
        when(aiOrchestratorService.answerChat(any())).thenReturn(ChatSendVO.builder()
                .answer("New answer")
                .references(List.of())
                .build());
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);
        when(chatSessionMapper.updateById(any(ChatSession.class))).thenReturn(1);

        ChatSendVO result = chatService.regenerateMessage(1L, 20L);

        assertEquals(10L, result.getSessionId());
        assertEquals("Existing Session", result.getSessionTitle());
        assertEquals("New answer", result.getAnswer());
        verify(chatMessageMapper, times(1)).insert(any(ChatMessage.class));
    }

    @Test
    void feedbackMessage_updatesFeedbackForOwnedMessage() {
        ChatMessage assistant = new ChatMessage();
        assistant.setId(20L);
        assistant.setSessionId(10L);
        assistant.setUserId(1L);
        assistant.setRole("assistant");
        assistant.setContent("Answer");
        when(chatMessageMapper.selectById(20L)).thenReturn(assistant);
        when(chatMessageMapper.updateById(any(ChatMessage.class))).thenReturn(1);

        ChatMessageVO result = chatService.feedbackMessage(1L, 20L, "positive");

        assertEquals("positive", result.getFeedback());
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageMapper).updateById(captor.capture());
        assertEquals("positive", captor.getValue().getFeedback());
    }

    @Test
    void feedbackMessage_invalidValueThrows() {
        assertThrows(BusinessException.class, () -> chatService.feedbackMessage(1L, 20L, "maybe"));
        verify(chatMessageMapper, never()).updateById(any(ChatMessage.class));
    }

    @Test
    void uploadAttachment_validatesAndStoresPrivateAttachment() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "jd.pdf",
                "application/pdf",
                "content".getBytes());
        when(fileStorageService.store(StorageDirectory.ATTACHMENT, "jd.pdf", "content".getBytes(), "application/pdf"))
                .thenReturn(StoredFile.builder()
                        .storageKey("local://attachments/2026/06/20/file.pdf")
                        .relativePath("attachments/2026/06/20/file.pdf")
                        .contentType("application/pdf")
                        .size(7L)
                        .build());

        var result = chatService.uploadAttachment(1L, file);

        assertEquals("local://attachments/2026/06/20/file.pdf", result.getId());
        assertEquals("local://attachments/2026/06/20/file.pdf", result.getFileId());
        assertEquals("jd.pdf", result.getFilename());
        assertEquals("application/pdf", result.getContentType());
        assertEquals(7L, result.getSize());
        verify(uploadPolicyService).validate(StorageDirectory.ATTACHMENT, "jd.pdf", "application/pdf", 7L);
        verify(fileStorageService).store(StorageDirectory.ATTACHMENT, "jd.pdf", "content".getBytes(), "application/pdf");
    }
}
