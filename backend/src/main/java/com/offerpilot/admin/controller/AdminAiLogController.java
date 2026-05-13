package com.offerpilot.admin.controller;

import com.offerpilot.admin.dto.AdminAiLogSummaryVO;
import com.offerpilot.admin.dto.AdminAiLogVO;
import com.offerpilot.admin.service.AdminAiGovernanceService;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 调用日志", description = "管理员查看模型调用日志、延迟和异常情况")
@RestController
@RequestMapping("/api/admin/ai-logs")
@RequiredArgsConstructor
public class AdminAiLogController {

    private final AdminAiGovernanceService adminAiGovernanceService;

    @Operation(summary = "分页查询 AI 调用日志")
    @GetMapping
    public Result<PageResult<AdminAiLogVO>> list(
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String callType,
            @RequestParam(required = false) Integer success,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminAiGovernanceService.listAiLogs(scene, callType, success, keyword, pageNum, pageSize));
    }

    @Operation(summary = "AI 调用日志摘要")
    @GetMapping("/summary")
    public Result<AdminAiLogSummaryVO> summary() {
        return Result.success(adminAiGovernanceService.aiLogSummary());
    }
}
