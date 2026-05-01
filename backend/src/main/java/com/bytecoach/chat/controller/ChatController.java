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

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public Result<ChatSendVO> send(@Valid @RequestBody ChatSendRequest request) {
        return Result.success(chatService.send(currentUserId(), request));
    }

    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVO>> sessions(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(chatService.listSessions(currentUserId(), pageNum, pageSize));
    }

    @GetMapping("/messages/{sessionId}")
    public Result<List<ChatMessageVO>> messages(@PathVariable Long sessionId) {
        return Result.success(chatService.listMessages(currentUserId(), sessionId));
    }

    @DeleteMapping("/session/{sessionId}")
    public Result<Void> delete(@PathVariable Long sessionId) {
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
