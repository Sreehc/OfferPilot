package com.offerpilot.agent.service;

import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.vo.AgentRunVO;
import java.util.List;

public interface AgentRunService {

    AgentRunVO createRun(Long userId, AgentRunCreateRequest request);

    List<AgentRunVO> listRuns(Long userId);

    AgentRunVO detail(Long userId, Long runId);
}
