package com.offerpilot.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.entity.ChatMessage;
import com.offerpilot.chat.entity.ChatSession;
import com.offerpilot.chat.mapper.ChatMessageMapper;
import com.offerpilot.chat.mapper.ChatSessionMapper;
import com.offerpilot.chat.service.ChatService;
import com.offerpilot.chat.vo.ChatAttachmentVO;
import com.offerpilot.chat.vo.ChatMessageReferenceVO;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.vo.ContextSourceVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.common.storage.FileStorageService;
import com.offerpilot.common.storage.StorageDirectory;
import com.offerpilot.common.storage.StoredFile;
import com.offerpilot.common.storage.UploadPolicyService;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private static final int CLIENT_MESSAGE_LOCK_STRIPES = 256;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final ObjectMapper objectMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final FileStorageService fileStorageService;
    private final UploadPolicyService uploadPolicyService;
    private final Object[] clientMessageLocks = createClientMessageLocks();

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ChatServiceImpl self;

    @Override
    public ChatSendVO send(Long userId, ChatSendRequest request) {
        return withClientMessageLock(userId, request, () -> sendInternal(userId, request));
    }

    private ChatSendVO sendInternal(Long userId, ChatSendRequest request) {
        request.setUserId(userId);
        ChatContextSnapshot contextSnapshot = resolveContext(userId, request.getMode(), request.getKnowledgeScope(),
                request.getResumeId(), request.getProjectId());
        applyResolvedContext(request, contextSnapshot);
        // Phase 1: persist user message in its own transaction (via proxy)
        PersistedChatTurn turn = self.persistUserMessage(userId, request);
        if (turn.existingAssistantMessage() != null) {
            return toExistingSendVO(turn.session(), request, contextSnapshot, turn.existingAssistantMessage());
        }

        // Phase 2: call LLM outside any transaction
        ChatSendVO result = aiOrchestratorService.answerChat(request);

        // Phase 3: persist assistant message and update session (via proxy)
        self.persistAssistantMessage(turn.session(), userId, result, request.getClientMessageId());
        result.setSessionId(turn.session().getId());
        result.setClientMessageId(request.getClientMessageId());
        result.setSessionTitle(turn.session().getTitle());
        result.setContextType(contextSnapshot.type());
        result.setContextSource(contextSnapshot.source());
        return result;
    }

    @Override
    public ChatSendVO streamChat(Long userId, ChatSendRequest request, Consumer<String> onToken) {
        return withClientMessageLock(userId, request, () -> streamChatInternal(userId, request, onToken));
    }

    private ChatSendVO streamChatInternal(Long userId, ChatSendRequest request, Consumer<String> onToken) {
        request.setUserId(userId);
        ChatContextSnapshot contextSnapshot = resolveContext(userId, request.getMode(), request.getKnowledgeScope(),
                request.getResumeId(), request.getProjectId());
        applyResolvedContext(request, contextSnapshot);
        // Phase 1: persist user message
        PersistedChatTurn turn = self.persistUserMessage(userId, request);
        if (turn.existingAssistantMessage() != null) {
            ChatSendVO existing = toExistingSendVO(turn.session(), request, contextSnapshot, turn.existingAssistantMessage());
            if (StringUtils.hasText(existing.getAnswer())) {
                onToken.accept(existing.getAnswer());
            }
            return existing;
        }

        // Phase 2: stream LLM response, accumulating full answer
        StringBuilder fullAnswer = new StringBuilder();
        Consumer<String> wrappedToken = token -> {
            fullAnswer.append(token);
            onToken.accept(token);
        };

        List<ChatMessageReferenceVO> references = aiOrchestratorService.streamChat(request, wrappedToken);

        // Phase 3: persist the complete assistant message
        ChatSendVO result = ChatSendVO.builder()
                .sessionId(turn.session().getId())
                .clientMessageId(request.getClientMessageId())
                .sessionTitle(turn.session().getTitle())
                .answer(fullAnswer.toString())
                .answerMode(request.getAnswerMode())
                .knowledgeScope(request.getKnowledgeScope())
                .contextType(contextSnapshot.type())
                .contextSource(contextSnapshot.source())
                .references(references)
                .build();
        self.persistAssistantMessage(turn.session(), userId, result, request.getClientMessageId());

        return result;
    }

    private ChatSendVO withClientMessageLock(Long userId, ChatSendRequest request, Supplier<ChatSendVO> operation) {
        String clientMessageId = normalizeClientMessageId(request.getClientMessageId());
        request.setClientMessageId(clientMessageId);
        if (!StringUtils.hasText(clientMessageId)) {
            return operation.get();
        }
        String lockKey = userId + ":" + clientMessageId;
        Object lock = clientMessageLocks[Math.floorMod(lockKey.hashCode(), clientMessageLocks.length)];
        synchronized (lock) {
            return operation.get();
        }
    }

    private static Object[] createClientMessageLocks() {
        Object[] locks = new Object[CLIENT_MESSAGE_LOCK_STRIPES];
        for (int index = 0; index < locks.length; index++) {
            locks[index] = new Object();
        }
        return locks;
    }

    @Transactional
    public PersistedChatTurn persistUserMessage(Long userId, ChatSendRequest request) {
        String clientMessageId = normalizeClientMessageId(request.getClientMessageId());
        request.setClientMessageId(clientMessageId);
        if (StringUtils.hasText(clientMessageId)) {
            ChatMessage existingUserMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getUserId, userId)
                    .eq(ChatMessage::getRole, "user")
                    .eq(ChatMessage::getClientMessageId, clientMessageId)
                    .orderByDesc(ChatMessage::getId)
                    .last("LIMIT 1"));
            if (existingUserMessage != null) {
                ChatSession existingSession = getOwnedSession(userId, existingUserMessage.getSessionId());
                applySessionDefaults(request, existingSession);
                ChatMessage existingAssistant = findExistingAssistantMessage(userId, existingSession.getId(), existingUserMessage);
                return new PersistedChatTurn(existingSession, existingUserMessage, existingAssistant);
            }
        }
        ChatSession session = request.getSessionId() == null
                ? createSession(userId, request.getMessage(), request.getMode())
                : getOwnedSession(userId, request.getSessionId());
        applySessionDefaults(request, session);
        if (!java.util.Objects.equals(session.getMode(), request.getMode())) {
            session.setMode(request.getMode());
        }
        session.setContextType(request.getContextType());
        session.setKnowledgeScope(request.getKnowledgeScope());
        session.setResumeFileId(request.getResumeId());
        session.setResumeProjectId(request.getProjectId());
        chatSessionMapper.updateById(session);
        ChatMessage userMessage = persistMessage(session.getId(), userId, "user", "text", request.getMessage(), null, clientMessageId);
        return new PersistedChatTurn(session, userMessage, null);
    }

    @Transactional
    public void persistAssistantMessage(ChatSession session, Long userId, ChatSendVO result) {
        persistAssistantMessage(session, userId, result, null);
    }

    @Transactional
    public void persistAssistantMessage(ChatSession session, Long userId, ChatSendVO result, String clientMessageId) {
        persistMessage(session.getId(), userId, "assistant",
                result.getReferences() == null || result.getReferences().isEmpty() ? "text" : "reference",
                result.getAnswer(), result.getReferences(), normalizeClientMessageId(clientMessageId));
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
                .map(this::toSessionVO)
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
    public ChatSendVO regenerateMessage(Long userId, Long messageId) {
        ChatMessage assistantMessage = getOwnedMessage(userId, messageId);
        if (!"assistant".equalsIgnoreCase(assistantMessage.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "only assistant messages can be regenerated");
        }
        ChatSession session = getOwnedSession(userId, assistantMessage.getSessionId());
        ChatMessage userMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, session.getId())
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getRole, "user")
                .lt(ChatMessage::getId, assistantMessage.getId())
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
        if (userMessage == null || !StringUtils.hasText(userMessage.getContent())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "source user message not found");
        }

        ChatSendRequest request = new ChatSendRequest();
        request.setSessionId(session.getId());
        request.setUserId(userId);
        request.setMessage(userMessage.getContent());
        request.setMode(StringUtils.hasText(session.getMode()) ? session.getMode() : "chat");
        request.setAnswerMode("learning");
        request.setKnowledgeScope(session.getKnowledgeScope());
        request.setResumeId(session.getResumeFileId());
        request.setProjectId(session.getResumeProjectId());

        ChatContextSnapshot contextSnapshot = resolveContext(userId, request.getMode(), request.getKnowledgeScope(),
                request.getResumeId(), request.getProjectId());
        applyResolvedContext(request, contextSnapshot);

        ChatSendVO result = aiOrchestratorService.answerChat(request);
        result.setSessionId(session.getId());
        result.setSessionTitle(session.getTitle());
        result.setContextType(contextSnapshot.type());
        result.setContextSource(contextSnapshot.source());
        self.persistAssistantMessage(session, userId, result);
        return result;
    }

    @Override
    @Transactional
    public ChatMessageVO feedbackMessage(Long userId, Long messageId, String feedback) {
        String normalized = StringUtils.hasText(feedback) ? feedback.trim().toLowerCase() : "";
        if (!"positive".equals(normalized) && !"negative".equals(normalized)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR.getCode(), "feedback must be positive or negative");
        }
        ChatMessage message = getOwnedMessage(userId, messageId);
        if (!"assistant".equalsIgnoreCase(message.getRole())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "only assistant messages accept feedback");
        }
        message.setFeedback(normalized);
        chatMessageMapper.updateById(message);
        return toVO(message);
    }

    @Override
    @Transactional
    public ChatSessionVO renameSession(Long userId, Long sessionId, String title) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR.getCode(), "session title cannot be blank");
        }
        ChatSession session = getOwnedSession(userId, sessionId);
        session.setTitle(title.trim());
        chatSessionMapper.updateById(session);
        return toSessionVO(session);
    }

    @Override
    public ChatAttachmentVO uploadAttachment(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        String originalFilename = file.getOriginalFilename();
        uploadPolicyService.validate(StorageDirectory.ATTACHMENT, originalFilename, file.getContentType(), file.getSize());
        try {
            StoredFile storedFile = fileStorageService.store(
                    StorageDirectory.ATTACHMENT,
                    originalFilename,
                    file.getBytes(),
                    file.getContentType());
            return ChatAttachmentVO.builder()
                    .id(storedFile.getStorageKey())
                    .fileId(storedFile.getStorageKey())
                    .filename(originalFilename)
                    .contentType(storedFile.getContentType())
                    .size(storedFile.getSize())
                    .build();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR.getCode(), "附件上传失败: " + e.getMessage());
        }
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

    private void applySessionDefaults(ChatSendRequest request, ChatSession session) {
        if (!StringUtils.hasText(request.getKnowledgeScope()) && StringUtils.hasText(session.getKnowledgeScope())) {
            request.setKnowledgeScope(session.getKnowledgeScope());
        }
        if (request.getResumeId() == null && session.getResumeFileId() != null) {
            request.setResumeId(session.getResumeFileId());
        }
        if (request.getProjectId() == null && session.getResumeProjectId() != null) {
            request.setProjectId(session.getResumeProjectId());
        }
    }

    private ChatMessage findExistingAssistantMessage(Long userId, Long sessionId, ChatMessage userMessage) {
        if (StringUtils.hasText(userMessage.getClientMessageId())) {
            ChatMessage byClientMessageId = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getUserId, userId)
                    .eq(ChatMessage::getSessionId, sessionId)
                    .eq(ChatMessage::getRole, "assistant")
                    .eq(ChatMessage::getClientMessageId, userMessage.getClientMessageId())
                    .orderByAsc(ChatMessage::getId)
                    .last("LIMIT 1"));
            if (byClientMessageId != null) {
                return byClientMessageId;
            }
        }
        if (userMessage.getId() == null) {
            return null;
        }
        return chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getRole, "assistant")
                .gt(ChatMessage::getId, userMessage.getId())
                .orderByAsc(ChatMessage::getId)
                .last("LIMIT 1"));
    }

    private ChatSendVO toExistingSendVO(ChatSession session, ChatSendRequest request,
                                        ChatContextSnapshot contextSnapshot, ChatMessage assistantMessage) {
        return ChatSendVO.builder()
                .sessionId(session.getId())
                .clientMessageId(request.getClientMessageId())
                .sessionTitle(session.getTitle())
                .answer(assistantMessage.getContent())
                .answerMode(request.getAnswerMode())
                .knowledgeScope(request.getKnowledgeScope())
                .contextType(contextSnapshot.type())
                .contextSource(contextSnapshot.source())
                .references(parseReferences(assistantMessage.getReferenceJson()))
                .build();
    }

    private String normalizeClientMessageId(String clientMessageId) {
        if (!StringUtils.hasText(clientMessageId)) {
            return null;
        }
        String normalized = clientMessageId.trim();
        return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
    }

    private ChatSession getOwnedSession(Long userId, Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "chat session not found");
        }
        return session;
    }

    private ChatMessage getOwnedMessage(Long userId, Long messageId) {
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null || !message.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "chat message not found");
        }
        return message;
    }

    private void persistMessage(Long sessionId, Long userId, String role, String messageType, String content,
                                List<ChatMessageReferenceVO> references) {
        persistMessage(sessionId, userId, role, messageType, content, references, null);
    }

    private ChatMessage persistMessage(Long sessionId, Long userId, String role, String messageType, String content,
                                       List<ChatMessageReferenceVO> references, String clientMessageId) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setReferenceJson(toReferenceJson(references));
        message.setClientMessageId(normalizeClientMessageId(clientMessageId));
        chatMessageMapper.insert(message);
        return message;
    }

    private ChatMessageVO toVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .clientMessageId(message.getClientMessageId())
                .role(message.getRole())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .feedback(message.getFeedback())
                .createTime(message.getCreateTime())
                .references(parseReferences(message.getReferenceJson()))
                .build();
    }

    private ChatSessionVO toSessionVO(ChatSession session) {
        return ChatSessionVO.builder()
                .id(session.getId())
                .title(session.getTitle())
                .mode(session.getMode())
                .contextType(inferContextType(session))
                .knowledgeScope(session.getKnowledgeScope())
                .contextSource(buildContextSource(session))
                .lastMessageTime(session.getLastMessageTime())
                .updateTime(session.getUpdateTime())
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

    private void applyResolvedContext(ChatSendRequest request, ChatContextSnapshot contextSnapshot) {
        request.setContextType(contextSnapshot.type());
        request.setKnowledgeScope(contextSnapshot.knowledgeScope());
        request.setResumeId(contextSnapshot.resumeId());
        request.setProjectId(contextSnapshot.projectId());
        request.setContextSummary(contextSnapshot.summary());
    }

    private ChatContextSnapshot resolveContext(Long userId, String mode, String knowledgeScope, Long resumeId, Long projectId) {
        if (projectId != null) {
            ResumeProject project = resumeProjectMapper.selectById(projectId);
            if (project == null || !project.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume project not found");
            }
            ResumeFile resume = resumeFileMapper.selectById(project.getResumeFileId());
            if (resume == null || !resume.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume file not found");
            }
            ContextSourceVO source = ContextSourceVO.builder()
                    .type("project")
                    .label("项目上下文")
                    .summary(buildProjectSummary(project, resume))
                    .knowledgeScope(normalizeKnowledgeScope(knowledgeScope))
                    .resumeId(resume.getId())
                    .resumeTitle(resume.getTitle())
                    .projectId(project.getId())
                    .projectName(project.getProjectName())
                    .build();
            return new ChatContextSnapshot("project", source, source.getSummary(), source.getKnowledgeScope(), resume.getId(), project.getId());
        }
        if (resumeId != null) {
            ResumeFile resume = resumeFileMapper.selectById(resumeId);
            if (resume == null || !resume.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "resume file not found");
            }
            String summary = buildResumeSummary(resume);
            ContextSourceVO source = ContextSourceVO.builder()
                    .type("resume")
                    .label("简历上下文")
                    .summary(summary)
                    .knowledgeScope(normalizeKnowledgeScope(knowledgeScope))
                    .resumeId(resume.getId())
                    .resumeTitle(resume.getTitle())
                    .build();
            return new ChatContextSnapshot("resume", source, summary, source.getKnowledgeScope(), resume.getId(), null);
        }
        if ("rag".equalsIgnoreCase(mode)) {
            String normalizedScope = normalizeKnowledgeScope(knowledgeScope);
            ContextSourceVO source = ContextSourceVO.builder()
                    .type("knowledge")
                    .label("资料上下文")
                    .summary("当前会优先基于" + knowledgeScopeLabel(normalizedScope) + "里的资料回答。")
                    .knowledgeScope(normalizedScope)
                    .build();
            return new ChatContextSnapshot("knowledge", source, source.getSummary(), normalizedScope, null, null);
        }
        ContextSourceVO source = ContextSourceVO.builder()
                .type("general")
                .label("自由提问")
                .summary("当前不会绑定特定资料、简历或项目，适合直接追问原理、场景和表达。")
                .build();
        return new ChatContextSnapshot("general", source, source.getSummary(), normalizeKnowledgeScope(knowledgeScope), null, null);
    }

    private ContextSourceVO buildContextSource(ChatSession session) {
        String contextType = inferContextType(session);
        String knowledgeScope = normalizeKnowledgeScope(session.getKnowledgeScope());
        if ("project".equals(contextType) && session.getResumeProjectId() != null) {
            ResumeProject project = resumeProjectMapper.selectById(session.getResumeProjectId());
            ResumeFile resume = session.getResumeFileId() == null ? null : resumeFileMapper.selectById(session.getResumeFileId());
            if (project != null) {
                return ContextSourceVO.builder()
                        .type("project")
                        .label("项目上下文")
                        .summary(buildProjectSummary(project, resume))
                        .knowledgeScope(knowledgeScope)
                        .resumeId(resume != null ? resume.getId() : null)
                        .resumeTitle(resume != null ? resume.getTitle() : null)
                        .projectId(project.getId())
                        .projectName(project.getProjectName())
                        .build();
            }
        }
        if ("resume".equals(contextType) && session.getResumeFileId() != null) {
            ResumeFile resume = resumeFileMapper.selectById(session.getResumeFileId());
            if (resume != null) {
                return ContextSourceVO.builder()
                        .type("resume")
                        .label("简历上下文")
                        .summary(buildResumeSummary(resume))
                        .knowledgeScope(knowledgeScope)
                        .resumeId(resume.getId())
                        .resumeTitle(resume.getTitle())
                        .build();
            }
        }
        if ("knowledge".equals(contextType)) {
            return ContextSourceVO.builder()
                    .type("knowledge")
                    .label("资料上下文")
                    .summary("当前会优先基于" + knowledgeScopeLabel(knowledgeScope) + "里的资料回答。")
                    .knowledgeScope(knowledgeScope)
                    .build();
        }
        return ContextSourceVO.builder()
                .type("general")
                .label("自由提问")
                .summary("当前不会绑定特定资料、简历或项目。")
                .knowledgeScope(knowledgeScope)
                .build();
    }

    private String inferContextType(ChatSession session) {
        if (StringUtils.hasText(session.getContextType())) {
            return session.getContextType();
        }
        if (session.getResumeProjectId() != null) {
            return "project";
        }
        if (session.getResumeFileId() != null) {
            return "resume";
        }
        if ("rag".equalsIgnoreCase(session.getMode())) {
            return "knowledge";
        }
        return "general";
    }

    private String normalizeKnowledgeScope(String knowledgeScope) {
        if ("system".equalsIgnoreCase(knowledgeScope) || "personal".equalsIgnoreCase(knowledgeScope)) {
            return knowledgeScope.toLowerCase();
        }
        return "all";
    }

    private String knowledgeScopeLabel(String knowledgeScope) {
        if ("system".equalsIgnoreCase(knowledgeScope)) {
            return "推荐资料";
        }
        if ("personal".equalsIgnoreCase(knowledgeScope)) {
            return "我的资料";
        }
        return "全部资料";
    }

    private String buildResumeSummary(ResumeFile resume) {
        String summary = StringUtils.hasText(resume.getSummary()) ? resume.getSummary() : "可结合这份简历里的经历、技术栈和项目成果来回答。";
        return "当前绑定简历《" + resume.getTitle() + "》。" + summary;
    }

    private String buildProjectSummary(ResumeProject project, ResumeFile resume) {
        String resumeLead = resume != null ? "，来源简历《" + resume.getTitle() + "》" : "";
        String techStack = StringUtils.hasText(project.getTechStack()) ? "技术栈：" + project.getTechStack() + "。" : "";
        String responsibility = StringUtils.hasText(project.getResponsibility()) ? "职责：" + project.getResponsibility() + "。" : "";
        String achievement = StringUtils.hasText(project.getAchievement()) ? "结果：" + project.getAchievement() + "。" : "";
        String summary = StringUtils.hasText(project.getProjectSummary()) ? project.getProjectSummary() : "优先围绕项目背景、职责、方案选择和结果来作答。";
        return "当前绑定项目「" + project.getProjectName() + "」" + resumeLead + "。" + summary + " " + techStack + responsibility + achievement;
    }

    private record ChatContextSnapshot(
            String type,
            ContextSourceVO source,
            String summary,
            String knowledgeScope,
            Long resumeId,
            Long projectId) {
    }

    public record PersistedChatTurn(
            ChatSession session,
            ChatMessage userMessage,
            ChatMessage existingAssistantMessage) {
    }
}
