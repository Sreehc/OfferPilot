package com.offerpilot.admin.controller;

import com.offerpilot.admin.dto.AdminRuntimeGovernanceSummaryVO;
import com.offerpilot.admin.service.AdminRuntimeGovernanceService;
import com.offerpilot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运行时治理", description = "管理员查看 agent、Copilot、转写与 provider 配置的运行状态")
@RestController
@RequestMapping("/api/admin/runtime-governance")
@RequiredArgsConstructor
public class AdminRuntimeGovernanceController {

    private final AdminRuntimeGovernanceService adminRuntimeGovernanceService;

    @Operation(summary = "运行时治理摘要")
    @GetMapping("/summary")
    public Result<AdminRuntimeGovernanceSummaryVO> summary() {
        return Result.success(adminRuntimeGovernanceService.summary());
    }
}
