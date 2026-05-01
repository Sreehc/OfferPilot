package com.bytecoach.auth.controller;

import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;
import com.bytecoach.auth.service.AuthService;
import com.bytecoach.common.api.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证管理", description = "用户注册、登录、登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册", description = "注册新用户并返回 Token")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "用户登录", description = "用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "用户登出", description = "将当前 Token 加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(
            @Parameter(description = "Bearer Token") @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return Result.success();
    }

    @Operation(summary = "健康检查")
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("ok");
    }
}
