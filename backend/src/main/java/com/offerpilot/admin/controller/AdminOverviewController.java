package com.offerpilot.admin.controller;

import com.offerpilot.admin.dto.AdminOverviewVO;
import com.offerpilot.admin.dto.AdminTrendVO;
import com.offerpilot.admin.service.AdminOverviewService;
import com.offerpilot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统概览", description = "管理员查看系统统计数据")
@RestController
@RequestMapping("/api/admin/overview")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final AdminOverviewService adminOverviewService;

    @Operation(summary = "系统概览统计")
    @GetMapping
    public Result<AdminOverviewVO> overview() {
        return Result.success(adminOverviewService.getOverview());
    }

    @Operation(summary = "近 30 天用户增长和活跃趋势")
    @GetMapping("/trend")
    public Result<List<AdminTrendVO>> trend() {
        return Result.success(adminOverviewService.getTrend());
    }
}
