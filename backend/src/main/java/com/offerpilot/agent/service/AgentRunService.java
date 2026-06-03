package com.offerpilot.agent.service;

import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.vo.AgentRunVO;
import java.util.List;

public interface AgentRunService {

    AgentRunVO createRun(Long userId, AgentRunCreateRequest request);

    List<AgentRunVO> listRuns(Long userId, String agentType, String status, String triggerSource,
                              String approvalStage, String providerGateStatus);

    AgentRunVO detail(Long userId, Long runId);

    AgentRunVO approveRun(Long userId, Long runId, String note);

    AgentRunVO rejectRun(Long userId, Long runId, String note);

    AgentRunVO cancelRun(Long userId, Long runId, String note);
}
