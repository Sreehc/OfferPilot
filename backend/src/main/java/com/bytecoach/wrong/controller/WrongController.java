package com.bytecoach.wrong.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.dto.PageResult;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import com.bytecoach.wrong.dto.WrongMasteryUpdateRequest;
import com.bytecoach.wrong.service.WrongService;
import com.bytecoach.wrong.vo.WrongQuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "错题本", description = "错题管理与掌握状态追踪")
@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongController {

    private final WrongService wrongService;

    @Operation(summary = "错题列表", description = "分页查询当前用户的错题")
    @GetMapping("/list")
    public Result<PageResult<WrongQuestionVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(wrongService.list(currentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "错题详情")
    @GetMapping("/{id}")
    public Result<WrongQuestionVO> detail(@Parameter(description = "错题 ID") @PathVariable Long id) {
        return Result.success(wrongService.detail(currentUserId(), id));
    }

    @Operation(summary = "更新掌握状态", description = "切换错题的掌握状态（未开始/复习中/已掌握）")
    @PutMapping("/mastery/{id}")
    public Result<Void> updateMastery(@Parameter(description = "错题 ID") @PathVariable Long id,
                                      @Valid @RequestBody WrongMasteryUpdateRequest request) {
        wrongService.updateMastery(currentUserId(), id, request);
        return Result.success();
    }

    @Operation(summary = "删除错题")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "错题 ID") @PathVariable Long id) {
        wrongService.delete(currentUserId(), id);
        return Result.success();
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
