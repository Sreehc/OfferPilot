package com.offerpilot.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.ai.service.AiOrchestratorService;
import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.entity.ChatMessage;
import com.offerpilot.chat.entity.ChatSession;
import com.offerpilot.chat.mapper.ChatMessageMapper;
import com.offerpilot.chat.mapper.ChatSessionMapper;
import com.offerpilot.chat.service.ChatService;
import com.offerpilot.chat.vo.ChatMessageReferenceVO;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.vo.ContextSourceVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiOrchestratorService aiOrchestratorService;
    private final ObjectMapper objectMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;

    @Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ChatServiceImpl self;

    @Override
    public ChatSendVO send(Long userId, ChatSendRequest request) {
        request.setUserId(userId);
        ChatContextSnapshot contextSnapshot = resolveContext(userId, request.getMode(), request.getKnowledgeScope(),
                request.getResumeId(), request.getProjectId());
        applyResolvedContext(request, contextSnapshot);
        // Phase 1: persist user message in its own transaction (via proxy)
        ChatSession session = self.persistUserMessage(userId, request);

        // Phase 2: call LLM outside any transaction
        ChatSendVO result = aiOrchestratorService.answerChat(request);

        // Phase 3: persist assistant message and update session (via proxy)
        self.persistAssistantMessage(session, userId, result);
        result.setSessionId(session.getId());
        result.setSessionTitle(session.getTitle());
        result.setContextType(contextSnapshot.type());
        result.setContextSource(contextSnapshot.source());
        return result;
    }

    @Override
    public ChatSendVO streamChat(Long userId, ChatSendRequest request, Consumer<String> onToken) {
        request.setUserId(userId);
        ChatContextSnapshot contextSnapshot = resolveContext(userId, request.getMode(), request.getKnowledgeScope(),
                request.getResumeId(), request.getProjectId());
        applyResolvedContext(request, contextSnapshot);
        // Phase 1: persist user message
        ChatSession session = self.persistUserMessage(userId, request);

        // Phase 2: stream LLM response, accumulating full answer
        StringBuilder fullAnswer = new StringBuilder();
        Consumer<String> wrappedToken = token -> {
            fullAnswer.append(token);
            onToken.accept(token);
        };

        List<ChatMessageReferenceVO> references = aiOrchestratorService.streamChat(request, wrappedToken);

        // Phase 3: persist the complete assistant message
        ChatSendVO result = ChatSendVO.builder()
                .sessionId(session.getId())
                .sessionTitle(session.getTitle())
                .answer(fullAnswer.toString())
                .answerMode(request.getAnswerMode())
                .knowledgeScope(request.getKnowledgeScope())
                .contextType(contextSnapshot.type())
                .contextSource(contextSnapshot.source())
                .references(references)
                .build();
        self.persistAssistantMessage(session, userId, result);

        return result;
    }

    @Transactional
    public ChatSession persistUserMessage(Long userId, ChatSendRequest request) {
        ChatSession session = request.getSessionId() == null
                ? createSession(userId, request.getMessage(), request.getMode())
                : getOwnedSession(userId, request.getSessionId());
        if (!StringUtils.hasText(request.getKnowledgeScope()) && StringUtils.hasText(session.getKnowledgeScope())) {
            request.setKnowledgeScope(session.getKnowledgeScope());
        }
        if (request.getResumeId() == null && session.getResumeFileId() != null) {
            request.setResumeId(session.getResumeFileId());
        }
        if (request.getProjectId() == null && session.getResumeProjectId() != null) {
            request.setProjectId(session.getResumeProjectId());
        }
        if (!session.getMode().equals(request.getMode())) {
            session.setMode(request.getMode());
        }
        session.setContextType(request.getContextType());
        session.setKnowledgeScope(request.getKnowledgeScope());
        session.setResumeFileId(request.getResumeId());
        session.setResumeProjectId(request.getProjectId());
        chatSessionMapper.updateById(session);
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
                        .contextType(inferContextType(session))
                        .knowledgeScope(session.getKnowledgeScope())
                        .contextSource(buildContextSource(session))
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
}
