package com.bytecoach.auth.controller;

import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.TwoFactorCodeRequest;
import com.bytecoach.auth.dto.TwoFactorEnableVO;
import com.bytecoach.auth.dto.TwoFactorSetupVO;
import com.bytecoach.auth.dto.TwoFactorStatusVO;
import com.bytecoach.auth.dto.TwoFactorVerifyRequest;
import com.bytecoach.auth.service.TwoFactorService;
import com.bytecoach.common.api.Result;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "两步验证", description = "TOTP 两步验证设置与验证")
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @Operation(summary = "获取两步验证状态")
    @GetMapping("/status")
    public Result<TwoFactorStatusVO> status() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(twoFactorService.getStatus(userId));
    }

    @Operation(summary = "初始化两步验证", description = "生成 TOTP secret 和 otpauth URI")
    @PostMapping("/setup")
    public Result<TwoFactorSetupVO> setup() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(twoFactorService.setup(userId));
    }

    @Operation(summary = "启用两步验证", description = "验证首次 TOTP code 后启用，返回 recovery codes")
    @PostMapping("/enable")
    public Result<TwoFactorEnableVO> enable(@Valid @RequestBody TwoFactorCodeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(twoFactorService.enable(userId, request.getCode()));
    }

    @Operation(summary = "关闭两步验证", description = "验证当前 TOTP code 后关闭")
    @PostMapping("/disable")
    public Result<Void> disable(@Valid @RequestBody TwoFactorCodeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        twoFactorService.disable(userId, request.getCode());
        return Result.success();
    }

    @Operation(summary = "两步验证登录验证", description = "使用 TOTP code 或 recovery code 完成登录")
    @PostMapping("/verify")
    public Result<LoginResponse> verify(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return Result.success(twoFactorService.verify(request.getTempToken(), request.getCode()));
    }
}
