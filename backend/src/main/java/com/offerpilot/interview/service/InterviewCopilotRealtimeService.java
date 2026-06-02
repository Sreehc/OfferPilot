package com.offerpilot.interview.service;

import com.offerpilot.interview.dto.CopilotRealtimeSessionCreateRequest;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;

public interface InterviewCopilotRealtimeService {

    CopilotRealtimeSessionVO createSession(Long userId, CopilotRealtimeSessionCreateRequest request);

    CopilotRealtimeSessionVO detail(Long userId, Long sessionId);

    CopilotRealtimeSessionVO connect(Long userId, Long sessionId);

    CopilotRealtimeSessionVO disconnect(Long userId, Long sessionId, String reason);

    CopilotRealtimeSessionVO complete(Long userId, Long sessionId, String summary);

    CopilotRealtimeSessionVO appendClientNote(Long userId, Long sessionId, String note);
}
