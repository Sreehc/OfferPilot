package com.offerpilot.plan.controller;

import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.plan.dto.StudyPlanGenerateRequest;
import com.offerpilot.plan.dto.StudyPlanTaskStatusRequest;
import com.offerpilot.plan.service.PlanService;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学习计划", description = "7/14/30 天学习计划与每日任务")
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "生成学习计划")
    @PostMapping("/generate")
    public Result<StudyPlanCurrentVO> generate(@Valid @RequestBody StudyPlanGenerateRequest request) {
        return Result.success(planService.generate(currentUserId(), request));
    }

    @Operation(summary = "当前学习计划")
    @GetMapping("/current")
    public Result<StudyPlanCurrentVO> current() {
        return Result.success(planService.current(currentUserId()));
    }

    @Operation(summary = "更新任务状态")
    @PostMapping("/task/{taskId}/status")
    public Result<StudyPlanCurrentVO> updateTaskStatus(
            @Parameter(description = "任务 ID") @PathVariable Long taskId,
            @Valid @RequestBody StudyPlanTaskStatusRequest request) {
        return Result.success(planService.updateTaskStatus(currentUserId(), taskId, request));
    }

    @Operation(summary = "刷新学习计划")
    @PostMapping("/{planId}/refresh")
    public Result<StudyPlanCurrentVO> refresh(@Parameter(description = "计划 ID") @PathVariable Long planId) {
        return Result.success(planService.refresh(currentUserId(), planId));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
