package com.bytecoach.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.adaptive.vo.AbilityProfileVO;
import com.bytecoach.analytics.service.AnalyticsService;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.cards.entity.KnowledgeCardTask;
import com.bytecoach.cards.mapper.KnowledgeCardTaskMapper;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.config.ByteCoachProperties;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.dashboard.dto.DashboardOverviewVO;
import com.bytecoach.dashboard.dto.RecentInterviewVO;
import com.bytecoach.dashboard.dto.WeakPointVO;
import com.bytecoach.dashboard.mapper.DashboardMetricsMapper;
import com.bytecoach.dashboard.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final AdaptiveService adaptiveService;
    private final AnalyticsService analyticsService;
    private final ByteCoachProperties props;
    private final KnowledgeCardTaskMapper knowledgeCardTaskMapper;

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
        List<RecentInterviewVO> recentInterviews = defaultList(dashboardMetricsMapper.selectRecentInterviews(userId));
        List<WeakPointVO> weakPoints = defaultList(dashboardMetricsMapper.selectWeakPoints(userId));
        MemorySummary memorySummary = loadMemorySummary(userId);

        DashboardOverviewVO result = DashboardOverviewVO.builder()
                .learningCount(learningCount)
                .averageScore(averageScore)
                .wrongCount(wrongCount)
                .recentInterviews(recentInterviews)
                .weakPoints(weakPoints)
                .firstVisit(learningCount == 0 && wrongCount == 0)
                .todayLearnCards(memorySummary.todayLearnCards())
                .todayReviewCards(memorySummary.todayReviewCards())
                .todayCompletedCards(memorySummary.todayCompletedCards())
                .todayCardCompletionRate(memorySummary.todayCardCompletionRate())
                .masteredCardCount(memorySummary.masteredCardCount())
                .reviewDebtCount(memorySummary.reviewDebtCount())
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
     * wrong book updates, or other dashboard-affecting changes.
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

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private MemorySummary loadMemorySummary(Long userId) {
        Long latestTaskId = dashboardMetricsMapper.selectLatestCardTaskId(userId);
        int reviewDebtCount = (int) defaultLong(dashboardMetricsMapper.countReviewDebt(userId));
        if (latestTaskId == null) {
            return new MemorySummary(0, 0, 0, BigDecimal.ZERO, 0, reviewDebtCount);
        }

        KnowledgeCardTask task = knowledgeCardTaskMapper.selectById(latestTaskId);
        if (task == null) {
            return new MemorySummary(0, 0, 0, BigDecimal.ZERO, 0, reviewDebtCount);
        }

        int currentDay = resolveCurrentDay(task);
        int todayLearnCards = (int) defaultLong(dashboardMetricsMapper.countTodayLearnCards(task.getId(), currentDay));
        int todayReviewCards = (int) defaultLong(dashboardMetricsMapper.countTodayReviewCards(task.getId(), currentDay));
        int todayCompletedCards = (int) defaultLong(dashboardMetricsMapper.countTodayCompletedCards(task.getId()));
        int masteredCardCount = (int) defaultLong(dashboardMetricsMapper.countMasteredCards(task.getId()));
        int todayCardTotal = todayLearnCards + todayReviewCards;
        BigDecimal todayCardCompletionRate = todayCardTotal <= 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(todayCompletedCards)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(todayCardTotal), 2, RoundingMode.HALF_UP);

        return new MemorySummary(
                todayLearnCards,
                todayReviewCards,
                todayCompletedCards,
                todayCardCompletionRate,
                masteredCardCount,
                reviewDebtCount
        );
    }

    private int resolveCurrentDay(KnowledgeCardTask task) {
        int totalDays = Math.max(1, task.getDays() == null ? 1 : task.getDays());
        int currentDay = task.getCurrentDay() == null ? 1 : task.getCurrentDay();
        if ("completed".equals(task.getStatus())) {
            return totalDays;
        }
        return Math.max(1, Math.min(currentDay, totalDays));
    }

    private record MemorySummary(
            int todayLearnCards,
            int todayReviewCards,
            int todayCompletedCards,
            BigDecimal todayCardCompletionRate,
            int masteredCardCount,
            int reviewDebtCount
    ) {
    }
}
