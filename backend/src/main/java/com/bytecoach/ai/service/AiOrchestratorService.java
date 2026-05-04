package com.bytecoach.ai.service;

import com.bytecoach.chat.dto.ChatSendRequest;
import com.bytecoach.chat.vo.ChatSendVO;
import com.bytecoach.chat.vo.ChatMessageReferenceVO;
import com.bytecoach.interview.dto.InterviewAnswerRequest;
import com.bytecoach.interview.vo.InterviewAnswerVO;
import java.util.List;
import java.util.function.Consumer;

public interface AiOrchestratorService {
    ChatSendVO answerChat(ChatSendRequest request);

    /**
     * Streaming chat. Calls onToken for each token, returns references when done.
     */
    List<ChatMessageReferenceVO> streamChat(ChatSendRequest request, Consumer<String> onToken);

    InterviewAnswerVO scoreInterviewAnswer(InterviewAnswerRequest request);
}
