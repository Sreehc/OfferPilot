package com.offerpilot.agent.controller;

import com.offerpilot.agent.dto.ProviderConfigUpdateRequest;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Provider 设置", description = "当前用户的模型与外部服务配置")
@RestController
@RequestMapping("/api/settings/providers")
@RequiredArgsConstructor
public class ProviderSettingsController {

    private final UserProviderConfigService userProviderConfigService;

    @Operation(summary = "查询当前用户 Provider 配置")
    @GetMapping
    public Result<List<UserProviderConfigItemVO>> list() {
        return Result.success(userProviderConfigService.listCurrentUserConfigs());
    }

    @Operation(summary = "更新当前用户 Provider 配置")
    @PutMapping
    public Result<List<UserProviderConfigItemVO>> update(@Valid @RequestBody ProviderConfigUpdateRequest request) {
        return Result.success(userProviderConfigService.updateCurrentUserConfigs(request.getConfigs()));
    }

    @Operation(summary = "重新检测当前用户 Provider 配置状态")
    @PostMapping("/check")
    public Result<List<UserProviderConfigItemVO>> check() {
        return Result.success(userProviderConfigService.checkCurrentUserConfigs());
    }
}
