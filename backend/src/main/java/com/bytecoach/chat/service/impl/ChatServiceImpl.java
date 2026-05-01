package com.bytecoach.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.service.AiOrchestratorService;
import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.entity.ChatMessage;
import com.bytecoach.chat.entity.ChatSession;
import com.bytecoach.chat.mapper.ChatMessageMapper;
import com.bytecoach.chat.mapper.ChatSessionMapper;
import com.bytecoach.chat.service.ChatService;
import com.bytecoach.chat.vo.ChatMessageReferenceVO;
import com.bytecoach.chat.vo.ChatMessageVO;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.chat.vo.ChatSessionVO;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final ObjectMapper objectMapper;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ChatServiceImpl self;

    @Override
    public ChatSendVO send(Long userId, ChatSendRequest request) {
        // Phase 1: persist user message in its own transaction (via proxy)
        ChatSession session = self.persistUserMessage(userId, request);

        // Phase 2: call LLM outside any transaction
        ChatSendVO result = aiOrchestratorService.answerChat(request);

        // Phase 3: persist assistant message and update session (via proxy)
        self.persistAssistantMessage(session, userId, result);
        result.setSessionId(session.getId());
        result.setSessionTitle(session.getTitle());
        return result;
    }

    @Transactional
    public ChatSession persistUserMessage(Long userId, ChatSendRequest request) {
        ChatSession session = request.getSessionId() == null
                ? createSession(userId, request.getMessage(), request.getMode())
                : getOwnedSession(userId, request.getSessionId());
        if (!session.getMode().equals(request.getMode())) {
            session.setMode(request.getMode());
        }
        persistMessage(session.getId(), userId, "user", "text", request.getMessage(), null);
        return session;
    }

    @Transactional
    public void persistAssistantMessage(ChatSession session, Long userId, ChatSendVO result) {
        persistMessage(session.getId(), userId, "assistant",
                result.getReferences() == null || result.getReferences().isEmpty() ? "text" : "reference",
                result.getAnswer(), result.getReferences());
        session.setTitle(refreshTitleIfNeeded(session.getTitle(), result.getAnswer()));
        session.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    @Override
    public PageResult<ChatSessionVO> listSessions(Long userId, int pageNum, int pageSize) {
        long total = chatSessionMapper.selectCount(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId));

        int offset = (Math.max(pageNum, 1) - 1) * Math.max(pageSize, 1);
        List<ChatSession> sessions = chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getLastMessageTime, ChatSession::getUpdateTime)
                .last("LIMIT " + Math.max(pageSize, 1) + " OFFSET " + offset));

        List<ChatSessionVO> voList = sessions.stream()
                .map(session -> ChatSessionVO.builder()
                        .id(session.getId())
                        .title(session.getTitle())
                        .mode(session.getMode())
                        .lastMessageTime(session.getLastMessageTime())
                        .updateTime(session.getUpdateTime())
                        .build())
                .toList();

        return PageResult.<ChatSessionVO>builder()
                .records(voList)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages((int) Math.ceil((double) total / Math.max(pageSize, 1)))
                .build();
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime, ChatMessage::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);
    }

    private ChatSession createSession(Long userId, String message, String mode) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(generateSessionTitle(message));
        session.setMode(mode);
        session.setLastMessageTime(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "chat session not found");
        }
        return session;
    }

    private void persistMessage(Long sessionId, Long userId, String role, String messageType, String content,
                                List<ChatMessageReferenceVO> references) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setReferenceJson(toReferenceJson(references));
        chatMessageMapper.insert(message);
    }

    private ChatMessageVO toVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .role(message.getRole())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .createTime(message.getCreateTime())
                .references(parseReferences(message.getReferenceJson()))
                .build();
    }

    private String toReferenceJson(List<ChatMessageReferenceVO> references) {
        if (references == null || references.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(references);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "failed to serialize references");
        }
    }

    private List<ChatMessageReferenceVO> parseReferences(String referenceJson) {
        if (referenceJson == null || referenceJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(referenceJson, new TypeReference<List<ChatMessageReferenceVO>>() {
            });
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "failed to parse references");
        }
    }

    private String refreshTitleIfNeeded(String currentTitle, String question) {
        if (currentTitle == null || currentTitle.isBlank() || currentTitle.startsWith("新会话")) {
            return generateSessionTitle(question);
        }
        return currentTitle;
    }

    private String generateSessionTitle(String message) {
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 18) {
            return normalized;
        }
        return normalized.substring(0, 18) + "...";
    }
}
