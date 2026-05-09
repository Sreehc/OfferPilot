package com.bytecoach.wrong.controller;

import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import com.bytecoach.wrong.dto.ReviewRateRequest;
import com.bytecoach.wrong.dto.ReviewStatsVO;
import com.bytecoach.wrong.dto.ReviewTodayVO;
import com.bytecoach.wrong.service.SpacedRepetitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "间隔复习", description = "基于 SM-2 算法的间隔复习引擎")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final SpacedRepetitionService spacedRepetitionService;

    @Operation(summary = "今日待复习", description = "返回当前用户今日及逾期的待复习题目，按优先级排序")
    @GetMapping("/today")
    public Result<ReviewTodayVO> today(
            @RequestParam(required = false, defaultValue = "all") String contentType) {
        return Result.success(spacedRepetitionService.getTodayReviews(currentUserId(), contentType));
    }

    @Operation(summary = "提交复习评分", description = "对一道统一复习项提交评分（1=Again, 2=Hard, 3=Good, 4=Easy）")
    @PostMapping("/{id}/rate")
    public Result<ReviewTodayVO> rate(
            @Parameter(description = "统一复习项 ID") @PathVariable Long id,
            @Valid @RequestBody ReviewRateRequest request) {
        return Result.success(spacedRepetitionService.rate(currentUserId(), id, request));
    }

    @Operation(summary = "复习统计", description = "获取复习统计数据：总复习次数、连续打卡天数、今日待复习/已完成数量")
    @GetMapping("/stats")
    public Result<ReviewStatsVO> stats() {
        return Result.success(spacedRepetitionService.getReviewStats(currentUserId()));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
