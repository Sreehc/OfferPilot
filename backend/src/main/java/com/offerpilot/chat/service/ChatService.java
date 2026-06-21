package com.offerpilot.chat.service;

import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.vo.ChatAttachmentVO;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.dto.PageResult;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {
    ChatSendVO send(Long userId, ChatSendRequest request);

    /**
     * Streaming chat. Persists user message, streams tokens via onToken,
     * then persists the complete assistant message.
     * Returns session info (sessionId, sessionTitle, references).
     */
    ChatSendVO streamChat(Long userId, ChatSendRequest request, Consumer<String> onToken);

    PageResult<ChatSessionVO> listSessions(Long userId, int pageNum, int pageSize);

    List<ChatMessageVO> listMessages(Long userId, Long sessionId);

    ChatSendVO regenerateMessage(Long userId, Long messageId);

    ChatMessageVO feedbackMessage(Long userId, Long messageId, String feedback);

    ChatSessionVO renameSession(Long userId, Long sessionId, String title);

    ChatAttachmentVO uploadAttachment(Long userId, MultipartFile file);

    void deleteSession(Long userId, Long sessionId);
}
