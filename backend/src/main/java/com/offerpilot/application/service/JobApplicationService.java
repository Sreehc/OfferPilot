package com.offerpilot.application.service;

import com.offerpilot.application.dto.JobApplicationCreateRequest;
import com.offerpilot.application.dto.JobApplicationEventCreateRequest;
import com.offerpilot.application.dto.JobApplicationStatusRequest;
import com.offerpilot.application.vo.JobApplicationVO;
import java.util.List;

public interface JobApplicationService {
    JobApplicationVO create(Long userId, JobApplicationCreateRequest request);

    List<JobApplicationVO> board(Long userId);

    JobApplicationVO detail(Long userId, Long applicationId);

    JobApplicationVO updateStatus(Long userId, Long applicationId, JobApplicationStatusRequest request);

    JobApplicationVO addEvent(Long userId, Long applicationId, JobApplicationEventCreateRequest request);

    JobApplicationVO analyze(Long userId, Long applicationId);
}
