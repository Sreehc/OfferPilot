package com.offerpilot.admin.controller;

import com.offerpilot.admin.dto.AdminInterviewGovernanceSummaryVO;
import com.offerpilot.admin.dto.AdminInterviewGovernanceVO;
import com.offerpilot.admin.service.AdminInterviewGovernanceService;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "面试治理", description = "管理员查看模拟面试会话质量、模式和低分情况")
@RestController
@RequestMapping("/api/admin/interviews")
@RequiredArgsConstructor
public class AdminInterviewGovernanceController {

    private final AdminInterviewGovernanceService adminInterviewGovernanceService;

    @Operation(summary = "分页查询面试治理列表")
    @GetMapping
    public Result<PageResult<AdminInterviewGovernanceVO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminInterviewGovernanceService.list(status, mode, keyword, pageNum, pageSize));
    }

    @Operation(summary = "面试治理摘要")
    @GetMapping("/summary")
    public Result<AdminInterviewGovernanceSummaryVO> summary() {
        return Result.success(adminInterviewGovernanceService.summary());
    }
}
