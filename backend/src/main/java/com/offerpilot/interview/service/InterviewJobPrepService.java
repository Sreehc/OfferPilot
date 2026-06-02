package com.offerpilot.interview.service;

import com.offerpilot.interview.dto.JobPrepSessionCreateRequest;
import com.offerpilot.interview.vo.JobPrepSessionVO;

public interface InterviewJobPrepService {

    JobPrepSessionVO createSession(Long userId, JobPrepSessionCreateRequest request);

    JobPrepSessionVO detail(Long userId, Long sessionId);
}
