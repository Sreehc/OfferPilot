package com.offerpilot.interview.service;

import com.offerpilot.interview.dto.VoiceStartRequest;
import com.offerpilot.interview.vo.InterviewCurrentQuestionVO;
import com.offerpilot.interview.vo.VoiceSubmitVO;

public interface InterviewVoiceService {

    /**
     * Start a voice interview session.
     * Creates a session with mode='voice' and returns the first question.
     */
    InterviewCurrentQuestionVO startVoice(Long userId, VoiceStartRequest request);

    /**
     * Submit audio answer for a voice interview question.
     * Transcribes audio via STT, then scores via AI.
     */
    VoiceSubmitVO submitVoice(Long userId, Long sessionId, Long questionId, byte[] audioData, String mimeType, String originalFilename);

    /**
     * Check if voice interview is available (STT configured).
     */
    boolean isVoiceAvailable();
}
