package com.bytecoach.plan.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.plan.dto.GeneratePlanRequest;
import com.bytecoach.plan.dto.PlanTaskStatusRequest;
import com.bytecoach.plan.service.PlanService;
import com.bytecoach.plan.vo.StudyPlanTaskVO;
import com.bytecoach.plan.vo.StudyPlanVO;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "学习计划", description = "AI 生成学习计划与任务追踪")
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @Operation(summary = "生成计划", description = "基于薄弱点 AI 生成 N 天学习计划")
    @PostMapping("/generate")
    public Result<StudyPlanVO> generate(@Valid @RequestBody GeneratePlanRequest request) {
        return Result.success(planService.generate(currentUserId(), request));
    }

    @Operation(summary = "当前计划", description = "获取当前激活的学习计划")
    @GetMapping("/current")
    public Result<StudyPlanVO> current() {
        return Result.success(planService.current(currentUserId()));
    }

    @Operation(summary = "计划任务列表")
    @GetMapping("/{id}/tasks")
    public Result<List<StudyPlanTaskVO>> tasks(@Parameter(description = "计划 ID") @PathVariable Long id) {
        return Result.success(planService.tasks(currentUserId(), id));
    }

    @Operation(summary = "更新任务状态", description = "标记任务为已完成或待办")
    @PutMapping("/task/{taskId}/status")
    public Result<Void> updateTaskStatus(@Parameter(description = "任务 ID") @PathVariable Long taskId,
                                         @Valid @RequestBody PlanTaskStatusRequest request) {
        planService.updateTaskStatus(currentUserId(), taskId, request);
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
