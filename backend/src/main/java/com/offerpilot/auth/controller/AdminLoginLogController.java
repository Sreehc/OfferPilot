package com.offerpilot.auth.controller;

import com.offerpilot.auth.dto.LoginLogVO;
import com.offerpilot.auth.service.LoginLogService;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登录日志管理", description = "管理员查看所有用户登录日志")
@RestController
@RequestMapping("/api/admin/login-logs")
@RequiredArgsConstructor
public class AdminLoginLogController {

    private final LoginLogService loginLogService;

    @Operation(summary = "查询所有用户登录日志", description = "管理员分页查询全部登录日志，支持按用户名搜索")
    @GetMapping
    public Result<PageResult<LoginLogVO>> listAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(loginLogService.listAll(keyword, pageNum, pageSize));
    }
}
