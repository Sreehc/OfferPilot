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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "间隔复习", description = "基于 SM-2 算法的间隔复习引擎")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final SpacedRepetitionService spacedRepetitionService;

    @Operation(summary = "今日待复习", description = "返回当前用户今日及逾期的待复习题目，按优先级排序")
    @GetMapping("/today")
    public Result<List<ReviewTodayVO>> today() {
        return Result.success(spacedRepetitionService.getTodayReviews(currentUserId()));
    }

    @Operation(summary = "提交复习评分", description = "对一道错题提交复习评分（1=Again, 2=Hard, 3=Good, 4=Easy），自动计算下次复习日期")
    @PostMapping("/{id}/rate")
    public Result<Void> rate(
            @Parameter(description = "错题 ID") @PathVariable Long id,
            @Valid @RequestBody ReviewRateRequest request) {
        spacedRepetitionService.rate(currentUserId(), id, request);
        return Result.success();
    }

    @Operation(summary = "复习统计", description = "获取复习统计数据：总复习次数、连续打卡天数、今日待复习/已完成数量")
    @GetMapping("/stats")
    public Result<ReviewStatsVO> stats() {
        Long userId = currentUserId();
        ReviewStatsVO stats = ReviewStatsVO.builder()
                .totalReviews(spacedRepetitionService.getTotalReviewCount(userId))
                .currentStreak(spacedRepetitionService.getReviewStreak(userId))
                .todayPending(spacedRepetitionService.getTodayReviewCount(userId))
                .build();
        return Result.success(stats);
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
