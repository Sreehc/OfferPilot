package com.offerpilot.ai.service;

import com.offerpilot.chat.dto.ChatSendRequest;
import com.offerpilot.chat.vo.ChatSendVO;
import com.offerpilot.chat.vo.ChatMessageReferenceVO;
import com.offerpilot.interview.dto.InterviewAnswerRequest;
import com.offerpilot.interview.vo.InterviewAnswerVO;
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
