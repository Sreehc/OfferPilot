package com.bytecoach.user.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户信息", description = "当前用户信息查询")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "当前用户", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(userService.getCurrentUserInfo());
    }
}

