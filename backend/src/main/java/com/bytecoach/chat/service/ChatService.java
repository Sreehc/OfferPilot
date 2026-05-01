package com.bytecoach.chat.service;

import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.vo.ChatMessageVO;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.chat.vo.ChatSessionVO;
import com.bytecoach.common.dto.PageResult;
import java.util.List;

public interface ChatService {
    ChatSendVO send(Long userId, ChatSendRequest request);

    PageResult<ChatSessionVO> listSessions(Long userId, int pageNum, int pageSize);

    List<ChatMessageVO> listMessages(Long userId, Long sessionId);

    void deleteSession(Long userId, Long sessionId);
}
