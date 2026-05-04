package com.bytecoach.admin.controller;

import com.bytecoach.admin.dto.AdminContentReviewVO;
import com.bytecoach.admin.service.AdminCommunityService;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "内容审核", description = "管理员操作：社区内容审核")
@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    @Operation(summary = "待审核内容列表")
    @GetMapping("/pending")
    public Result<PageResult<AdminContentReviewVO>> pending(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(adminCommunityService.listPending(pageNum, pageSize));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        adminCommunityService.approve(id);
        return Result.success();
    }

    @Operation(summary = "审核拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminCommunityService.reject(id, reason);
        return Result.success();
    }
}
