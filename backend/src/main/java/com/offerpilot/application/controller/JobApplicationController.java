package com.offerpilot.application.controller;

import com.offerpilot.application.dto.JobApplicationCreateRequest;
import com.offerpilot.application.dto.JobApplicationEventCreateRequest;
import com.offerpilot.application.dto.JobApplicationStatusRequest;
import com.offerpilot.application.service.JobApplicationService;
import com.offerpilot.application.vo.JobApplicationVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "投递管理", description = "岗位投递、JD 分析和真实面试复盘")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "创建投递")
    @PostMapping
    public Result<JobApplicationVO> create(@Valid @RequestBody JobApplicationCreateRequest request) {
        return Result.success(jobApplicationService.create(currentUserId(), request));
    }

    @Operation(summary = "投递看板")
    @GetMapping("/board")
    public Result<List<JobApplicationVO>> board() {
        return Result.success(jobApplicationService.board(currentUserId()));
    }

    @Operation(summary = "投递详情")
    @GetMapping("/{applicationId}")
    public Result<JobApplicationVO> detail(@Parameter(description = "投递 ID") @PathVariable Long applicationId) {
        return Result.success(jobApplicationService.detail(currentUserId(), applicationId));
    }

    @Operation(summary = "更新投递状态")
    @PutMapping("/{applicationId}/status")
    public Result<JobApplicationVO> updateStatus(
            @Parameter(description = "投递 ID") @PathVariable Long applicationId,
            @Valid @RequestBody JobApplicationStatusRequest request) {
        return Result.success(jobApplicationService.updateStatus(currentUserId(), applicationId, request));
    }

    @Operation(summary = "新增投递事件")
    @PostMapping("/{applicationId}/events")
    public Result<JobApplicationVO> addEvent(
            @Parameter(description = "投递 ID") @PathVariable Long applicationId,
            @Valid @RequestBody JobApplicationEventCreateRequest request) {
        return Result.success(jobApplicationService.addEvent(currentUserId(), applicationId, request));
    }

    @Operation(summary = "重做 JD 分析")
    @PostMapping("/{applicationId}/analysis")
    public Result<JobApplicationVO> analyze(@Parameter(description = "投递 ID") @PathVariable Long applicationId) {
        return Result.success(jobApplicationService.analyze(currentUserId(), applicationId));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
