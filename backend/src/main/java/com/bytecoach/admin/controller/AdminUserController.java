package com.bytecoach.admin.controller;

import com.bytecoach.admin.dto.AdminUserDetailVO;
import com.bytecoach.admin.dto.AdminUserUpdateRequest;
import com.bytecoach.admin.service.AdminUserService;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.user.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理", description = "管理员操作：用户列表、编辑、封禁/解封")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<PageResult<UserInfoVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminUserService.listUsers(keyword, role, pageNum, pageSize));
    }

    @Operation(summary = "修改用户信息")
    @PutMapping("/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateRequest request) {
        adminUserService.updateUser(id, request);
        return Result.success();
    }

    @Operation(summary = "封禁用户")
    @PostMapping("/{id}/ban")
    public Result<Void> banUser(@PathVariable Long id) {
        adminUserService.banUser(id);
        return Result.success();
    }

    @Operation(summary = "解封用户")
    @PostMapping("/{id}/unban")
    public Result<Void> unbanUser(@PathVariable Long id) {
        adminUserService.unbanUser(id);
        return Result.success();
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}/detail")
    public Result<AdminUserDetailVO> userDetail(@PathVariable Long id) {
        return Result.success(adminUserService.getUserDetail(id));
    }
}
