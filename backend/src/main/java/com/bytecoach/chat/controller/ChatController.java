package com.bytecoach.chat.controller;

import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.service.ChatService;
import com.bytecoach.chat.vo.ChatMessageVO;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.chat.vo.ChatSessionVO;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "智能问答", description = "Chat/RAG 智能问答会话管理")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发送消息", description = "向会话发送消息并获取 AI 回答")
    @PostMapping("/send")
    public Result<ChatSendVO> send(@Valid @RequestBody ChatSendRequest request) {
        return Result.success(chatService.send(currentUserId(), request));
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
