package com.offerpilot.chat.service;

import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.vo.ChatMessageVO;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatSessionVO;
import com.offerpilot.common.dto.PageResult;
import java.util.List;
import java.util.function.Consumer;

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

    void deleteSession(Long userId, Long sessionId);
}
