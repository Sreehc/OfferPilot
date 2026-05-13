package com.offerpilot.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.cards.entity.KnowledgeCard;
import com.offerpilot.cards.mapper.KnowledgeCardMapper;
import com.offerpilot.cards.entity.KnowledgeCardTask;
import com.offerpilot.cards.mapper.KnowledgeCardTaskMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.RecentInterviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.mapper.DashboardMetricsMapper;
import com.offerpilot.dashboard.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.offerpilot.security.util.SecurityUtils;

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
    private final OfferPilotProperties props;
    private final KnowledgeCardTaskMapper knowledgeCardTaskMapper;
    private final KnowledgeCardMapper knowledgeCardMapper;

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
                .studyStreak(memorySummary.studyStreak())
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
            EfficiencyVO efficiency = analyticsService.getEfficiencyData(userId);
            result.setThisWeekAvgScore(insights.getThisWeekAvgScore());
            result.setLastWeekAvgScore(insights.getLastWeekAvgScore());
            result.setThisWeekInterviewCount(insights.getThisWeekInterviewCount());
            result.setCategoryChanges(insights.getCategoryChanges());
            result.setBestStudyHours(insights.getBestStudyHours());
            result.setTodayCompletionStatus(insights.getTodayCompletionStatus());
            result.setCategoryMasterySummary(efficiency.getCategoryMastery());
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
        int reviewDebtCount = (int) defaultLong(dashboardMetricsMapper.countReviewDebt(userId));
        List<KnowledgeCardTask> tasks = knowledgeCardTaskMapper.selectList(new LambdaQueryWrapper<KnowledgeCardTask>()
                .eq(KnowledgeCardTask::getUserId, userId)
                .ne(KnowledgeCardTask::getStatus, "invalid"));
        if (tasks.isEmpty()) {
            return new MemorySummary(0, 0, 0, BigDecimal.ZERO, 0, reviewDebtCount, 0);
        }

        List<Long> taskIds = tasks.stream().map(KnowledgeCardTask::getId).toList();
        List<KnowledgeCard> cards = knowledgeCardMapper.selectList(new LambdaQueryWrapper<KnowledgeCard>()
                .in(KnowledgeCard::getTaskId, taskIds));
        Map<Long, Integer> currentDays = tasks.stream()
                .collect(java.util.stream.Collectors.toMap(KnowledgeCardTask::getId, this::resolveCurrentDay));

        int todayLearnCards = (int) cards.stream()
                .filter(card -> "new".equals(card.getState()))
                .filter(card -> card.getScheduledDay() != null && card.getScheduledDay() <= currentDays.getOrDefault(card.getTaskId(), 1))
                .count();
        int todayReviewCards = (int) cards.stream()
                .filter(card -> !"new".equals(card.getState()) && !"mastered".equals(card.getState()))
                .filter(card -> card.getNextReviewAt() == null || !card.getNextReviewAt().isAfter(java.time.LocalDateTime.now()))
                .count();
        int todayCompletedCards = (int) cards.stream()
                .filter(card -> card.getLastReviewTime() != null
                        && card.getLastReviewTime().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
        int masteredCardCount = (int) cards.stream()
                .filter(card -> "mastered".equals(card.getState()))
                .count();
        int studyStreak = resolveStudyStreak(cards);
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
                reviewDebtCount,
                studyStreak
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
            int reviewDebtCount,
            int studyStreak
    ) {
    }

    private int resolveStudyStreak(List<KnowledgeCard> cards) {
        java.util.Set<java.time.LocalDate> reviewedDates = cards.stream()
                .map(KnowledgeCard::getLastReviewTime)
                .filter(java.util.Objects::nonNull)
                .map(java.time.LocalDateTime::toLocalDate)
                .collect(java.util.stream.Collectors.toSet());
        if (reviewedDates.isEmpty()) {
            return 0;
        }
        java.time.LocalDate cursor = java.time.LocalDate.now();
        int streak = 0;
        while (reviewedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
