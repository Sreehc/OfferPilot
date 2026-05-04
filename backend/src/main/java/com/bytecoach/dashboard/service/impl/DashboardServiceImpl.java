package com.bytecoach.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.config.ByteCoachProperties;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.dashboard.dto.DashboardOverviewVO;
import com.bytecoach.dashboard.dto.RecentInterviewVO;
import com.bytecoach.dashboard.dto.WeakPointVO;
import com.bytecoach.dashboard.mapper.DashboardMetricsMapper;
import com.bytecoach.dashboard.service.DashboardService;
import com.bytecoach.plan.entity.StudyPlan;
import com.bytecoach.plan.mapper.StudyPlanMapper;
import com.bytecoach.plan.service.PlanAdjustService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.bytecoach.security.util.SecurityUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_PREFIX = "dashboard:overview:";

    private final DashboardMetricsMapper dashboardMetricsMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final PlanAdjustService planAdjustService;
    private final StudyPlanMapper planMapper;
    private final AdaptiveService adaptiveService;
    private final AnalyticsService analyticsService;
    private final ByteCoachProperties props;

    @Override
    public DashboardOverviewVO overview() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }

        // Try cache first
        String cacheKey = CACHE_PREFIX + userId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, DashboardOverviewVO.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read dashboard cache: {}", e.getMessage());
        }

        long chatCount = defaultLong(dashboardMetricsMapper.countChatSessions(userId));
        long interviewCount = defaultLong(dashboardMetricsMapper.countInterviewSessions(userId));
        int learningCount = (int) (chatCount + interviewCount);
        BigDecimal averageScore = defaultDecimal(dashboardMetricsMapper.averageInterviewScore(userId));
        int wrongCount = (int) defaultLong(dashboardMetricsMapper.countWrongQuestions(userId));
        int planCompletionRate = defaultInt(dashboardMetricsMapper.planCompletionRate(userId));
        List<RecentInterviewVO> recentInterviews = defaultList(dashboardMetricsMapper.selectRecentInterviews(userId));
        List<WeakPointVO> weakPoints = defaultList(dashboardMetricsMapper.selectWeakPoints(userId));

        // Calculate plan health score from active plan
        int planHealthScore = 100;
        StudyPlan activePlan = planMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(StudyPlan::getStatus, "active")
                .last("LIMIT 1"));
        if (activePlan != null) {
            planHealthScore = planAdjustService.calculateHealthScore(activePlan.getId());
        }

        DashboardOverviewVO result = DashboardOverviewVO.builder()
                .learningCount(learningCount)
                .averageScore(averageScore)
                .wrongCount(wrongCount)
                .planCompletionRate(planCompletionRate)
                .planHealthScore(planHealthScore)
                .recentInterviews(recentInterviews)
                .weakPoints(weakPoints)
                .firstVisit(learningCount == 0 && wrongCount == 0)
                .build();

        // Populate adaptive learning fields
        try {
            AbilityProfileVO profile = adaptiveService.getAbilityProfile(userId);
            result.setOverallAbility(profile.getOverallAbility());
            result.setRecommendedDifficulty(profile.getRecommendedDifficulty());
            result.setWeakCategories(profile.getWeakCategories());
            result.setSuggestedFocus(profile.getSuggestedFocus());
            result.setCategoryAbilities(profile.getCategoryAbilities());
        } catch (Exception e) {
            log.warn("Failed to load adaptive profile for dashboard: {}", e.getMessage());
        }

        // Populate analytics insights
        try {
            LearningInsightsVO insights = analyticsService.getLearningInsights(userId);
            result.setThisWeekAvgScore(insights.getThisWeekAvgScore());
            result.setLastWeekAvgScore(insights.getLastWeekAvgScore());
            result.setThisWeekInterviewCount(insights.getThisWeekInterviewCount());
            result.setCategoryChanges(insights.getCategoryChanges());
            result.setBestStudyHours(insights.getBestStudyHours());
        } catch (Exception e) {
            log.warn("Failed to load analytics insights for dashboard: {}", e.getMessage());
        }

        // Cache the result
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, props.getDashboard().getCacheTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to write dashboard cache: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Clear the dashboard cache for a user. Call this after interview completion,
     * wrong book updates, or plan changes.
     */
    public void evictCache(Long userId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to evict dashboard cache: {}", e.getMessage());
        }
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
