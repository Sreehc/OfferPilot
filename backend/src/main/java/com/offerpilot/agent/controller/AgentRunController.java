package com.offerpilot.agent.controller;

import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.service.AgentRunService;
import com.offerpilot.agent.vo.AgentRunVO;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agent 工作台", description = "统一任务发起、run 查看和结果消费")
@RestController
@RequestMapping("/api/agent/runs")
@RequiredArgsConstructor
public class AgentRunController {

    private final AgentRunService agentRunService;

    @Operation(summary = "创建 agent run", description = "统一入口，按 agentType 和 triggerSource 发起任务")
    @PostMapping
    public Result<AgentRunVO> create(@Valid @RequestBody AgentRunCreateRequest request) {
        return Result.success(agentRunService.createRun(currentUserId(), request));
    }

    @Operation(summary = "agent run 列表", description = "查看当前用户最近发起的 run")
    @GetMapping
    public Result<List<AgentRunVO>> list() {
        return Result.success(agentRunService.listRuns(currentUserId()));
    }

    @Operation(summary = "agent run 详情", description = "查看单个 run 的结果和下一步动作")
    @GetMapping("/{runId}")
    public Result<AgentRunVO> detail(@Parameter(description = "run ID") @PathVariable Long runId) {
        return Result.success(agentRunService.detail(currentUserId(), runId));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
