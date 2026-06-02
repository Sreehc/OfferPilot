package com.offerpilot.interview.service;

import com.offerpilot.interview.dto.CopilotPrepSessionCreateRequest;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;

public interface InterviewCopilotPrepService {

    CopilotPrepSessionVO createSession(Long userId, CopilotPrepSessionCreateRequest request);

    CopilotPrepSessionVO detail(Long userId, Long sessionId);
}
