package com.offerpilot.chat.controller;

import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.service.ChatService;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.dto.PageResult;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Tag(name = "智能问答", description = "Chat/RAG 智能问答会话管理")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @Operation(summary = "发送消息", description = "向会话发送消息并获取 AI 回答")
    @PostMapping("/send")
    public Result<ChatSendVO> send(@Valid @RequestBody ChatSendRequest request) {
        return Result.success(chatService.send(currentUserId(), request));
    }

    @Operation(summary = "流式发送消息", description = "SSE 流式返回 AI 回答，逐 token 推送")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatSendRequest request) {
        Long userId = currentUserId();
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout

        emitter.onCompletion(() -> log.debug("SSE stream completed for user {}", userId));
        emitter.onTimeout(() -> {
            log.warn("SSE stream timed out for user {}", userId);
            emitter.complete();
        });
        emitter.onError(e -> log.warn("SSE stream error for user {}: {}", userId, e.getMessage()));

        sseExecutor.execute(() -> {
            try {
                ChatSendVO result = chatService.streamChat(userId, request, token -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(Map.of("content", token)));
                    } catch (Exception e) {
                        log.debug("Failed to send SSE token: {}", e.getMessage());
                    }
                });

                // Send completion event with session info and references
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of(
                                "sessionId", result.getSessionId(),
                                "sessionTitle", result.getSessionTitle() != null ? result.getSessionTitle() : "",
                                "references", result.getReferences() != null ? result.getReferences() : List.of(),
                                "suggestedQuestions", result.getSuggestedQuestions() != null ? result.getSuggestedQuestions() : List.of(),
                                "answerMode", result.getAnswerMode() != null ? result.getAnswerMode() : "",
                                "knowledgeScope", result.getKnowledgeScope() != null ? result.getKnowledgeScope() : ""
                        )));
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE stream failed for user {}: {}", userId, e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data(Map.of("message", e.getMessage() != null ? e.getMessage() : "stream error")));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Operation(summary = "会话列表", description = "分页查询当前用户的会话列表")
    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVO>> sessions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chatService.listSessions(currentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "消息列表", description = "查询会话的所有消息记录")
    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMessageVO>> messages(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        return Result.success(chatService.listMessages(currentUserId(), sessionId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> delete(@Parameter(description = "会话 ID") @PathVariable Long sessionId) {
        chatService.deleteSession(currentUserId(), sessionId);
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
