package com.offerpilot.analytics.controller;

import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.TrendVO;
import com.offerpilot.common.api.Result;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据分析", description = "学习趋势与效率分析")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "能力趋势", description = "按周聚合计划、投递、简历与面试趋势，支持分类筛选")
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

    @Operation(summary = "学习洞察", description = "返回计划、投递、简历、面试和训练节奏洞察")
    @GetMapping("/insights")
    public Result<LearningInsightsVO> insights() {
        return Result.success(analyticsService.getLearningInsights(currentUserId()));
    }

    @Operation(summary = "能力画像", description = "返回长期能力画像、薄弱分类和建议难度")
    @GetMapping("/profile")
    public Result<AbilityProfileVO> profile() {
        return Result.success(analyticsService.getAbilityProfile(currentUserId()));
    }

    @Operation(summary = "能力画像领域详情", description = "返回指定分类的画像、掌握度和建议动作")
    @GetMapping("/profile/topics/{topicId}")
    public Result<ProfileTopicDetailVO> profileTopic(@Parameter(description = "分类 ID") @PathVariable Long topicId) {
        return Result.success(analyticsService.getProfileTopicDetail(currentUserId(), topicId));
    }

    private Long currentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return userId;
    }
}
