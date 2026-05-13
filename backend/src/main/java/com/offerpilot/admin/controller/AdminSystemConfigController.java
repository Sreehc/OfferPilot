package com.offerpilot.admin.controller;

import com.offerpilot.admin.dto.AdminSystemConfigUpdateRequest;
import com.offerpilot.admin.dto.AdminSystemConfigVO;
import com.offerpilot.admin.service.AdminAiGovernanceService;
import com.offerpilot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统配置", description = "管理员查看和维护 AI 运行配置与提示词模板")
@RestController
@RequestMapping("/api/admin/system-config")
@RequiredArgsConstructor
public class AdminSystemConfigController {

    private final AdminAiGovernanceService adminAiGovernanceService;

    @Operation(summary = "查询系统配置")
    @GetMapping
    public Result<List<AdminSystemConfigVO>> list() {
        return Result.success(adminAiGovernanceService.listSystemConfigs());
    }

    @Operation(summary = "更新单个系统配置")
    @PutMapping("/{configKey:.+}")
    public Result<AdminSystemConfigVO> update(
            @PathVariable String configKey,
            @Valid @RequestBody AdminSystemConfigUpdateRequest request) {
        return Result.success(adminAiGovernanceService.updateSystemConfig(configKey, request));
    }
}
