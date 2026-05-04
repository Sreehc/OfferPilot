package com.bytecoach.analytics.controller;

import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.analytics.vo.EfficiencyVO;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.analytics.vo.TrendVO;
import com.bytecoach.common.api.Result;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据分析", description = "学习趋势与效率分析")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "能力趋势", description = "按周聚合面试得分趋势，支持分类筛选")
    @GetMapping("/trend")
    public Result<TrendVO> trend(
            @RequestParam(defaultValue = "12") int weeks,
            @RequestParam(required = false) List<Long> categoryIds) {
        return Result.success(analyticsService.getAbilityTrend(currentUserId(), weeks, categoryIds));
    }

    @Operation(summary = "学习效率", description = "复习效率分析：EF趋势、遗忘率、掌握分布")
    @GetMapping("/efficiency")
    public Result<EfficiencyVO> efficiency() {
        return Result.success(analyticsService.getEfficiencyData(currentUserId()));
    }

    @Operation(summary = "学习洞察", description = "周对比、分类变化、最佳学习时段")
    @GetMapping("/insights")
    public Result<LearningInsightsVO> insights() {
        return Result.success(analyticsService.getLearningInsights(currentUserId()));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
