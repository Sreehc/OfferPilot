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
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.RecentInterviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.mapper.DashboardMetricsMapper;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.plan.entity.StudyPlan;
import com.offerpilot.plan.entity.StudyPlanTask;
import com.offerpilot.plan.mapper.StudyPlanMapper;
import com.offerpilot.plan.mapper.StudyPlanTaskMapper;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.entity.ResumeProject;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.resume.mapper.ResumeProjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
    private final StudyPlanMapper studyPlanMapper;
    private final StudyPlanTaskMapper studyPlanTaskMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final ResumeProjectMapper resumeProjectMapper;
    private final JobApplicationMapper jobApplicationMapper;

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
                .planSummary(loadPlanSummary(userId))
                .resumeSummary(loadResumeSummary(userId))
                .applicationSummary(loadApplicationSummary(userId))
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

        result.setNextStep(resolveNextStep(result));

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

    private DashboardOverviewVO.PlanSummary loadPlanSummary(Long userId) {
        StudyPlan plan = studyPlanMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .orderByDesc(StudyPlan::getUpdateTime)
                .last("LIMIT 1"));
        if (plan == null) {
            return DashboardOverviewVO.PlanSummary.builder()
                    .title("还没有训练计划")
                    .actionPath("/study-plan")
                    .build();
        }

        List<StudyPlanTask> tasks = studyPlanTaskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, plan.getId()));
        int todayTaskCount = (int) tasks.stream()
                .filter(task -> task.getDayIndex() != null && task.getDayIndex().equals(plan.getCurrentDay()))
                .count();

        return DashboardOverviewVO.PlanSummary.builder()
                .planId(plan.getId())
                .title(plan.getTitle())
                .currentDay(plan.getCurrentDay())
                .totalDays(plan.getDurationDays())
                .todayTaskCount(todayTaskCount)
                .completedTaskCount(plan.getCompletedTaskCount())
                .totalTaskCount(plan.getTotalTaskCount())
                .progressRate(defaultDecimal(plan.getProgressRate()))
                .actionPath("/study-plan")
                .build();
    }

    private DashboardOverviewVO.ResumeSummary loadResumeSummary(Long userId) {
        List<ResumeFile> resumes = resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getUpdateTime));
        if (resumes.isEmpty()) {
            return DashboardOverviewVO.ResumeSummary.builder()
                    .resumeCount(0)
                    .latestResumeTitle("还没有上传简历")
                    .projectCount(0)
                    .actionPath("/resume")
                    .build();
        }
        ResumeFile latestResume = resumes.get(0);
        int projectCount = Math.toIntExact(resumeProjectMapper.selectCount(new LambdaQueryWrapper<ResumeProject>()
                .eq(ResumeProject::getResumeFileId, latestResume.getId())));
        return DashboardOverviewVO.ResumeSummary.builder()
                .resumeId(latestResume.getId())
                .resumeCount(resumes.size())
                .latestResumeTitle(latestResume.getTitle())
                .parseStatus(latestResume.getParseStatus())
                .projectCount(projectCount)
                .actionPath("/resume")
                .build();
    }

    private DashboardOverviewVO.ApplicationSummary loadApplicationSummary(Long userId) {
        List<JobApplication> applications = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .orderByDesc(JobApplication::getUpdateTime));
        if (applications.isEmpty()) {
            return DashboardOverviewVO.ApplicationSummary.builder()
                    .totalCount(0)
                    .activeCount(0)
                    .offerCount(0)
                    .averageMatchScore(BigDecimal.ZERO)
                    .topCompany("还没有投递记录")
                    .actionPath("/applications")
                    .build();
        }

        int activeCount = (int) applications.stream()
                .filter(item -> List.of("applied", "written", "interview").contains(item.getStatus()))
                .count();
        int offerCount = (int) applications.stream()
                .filter(item -> "offer".equals(item.getStatus()))
                .count();
        BigDecimal averageMatchScore = applications.stream()
                .map(JobApplication::getMatchScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int scoreCount = (int) applications.stream().map(JobApplication::getMatchScore).filter(java.util.Objects::nonNull).count();
        BigDecimal avgScore = scoreCount > 0
                ? averageMatchScore.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        JobApplication topApplication = applications.stream()
                .filter(item -> item.getMatchScore() != null)
                .max(java.util.Comparator.comparing(JobApplication::getMatchScore))
                .orElse(applications.get(0));

        return DashboardOverviewVO.ApplicationSummary.builder()
                .totalCount(applications.size())
                .activeCount(activeCount)
                .offerCount(offerCount)
                .averageMatchScore(avgScore)
                .topCompany(topApplication.getCompany())
                .actionPath("/applications")
                .build();
    }

    private DashboardOverviewVO.NextStepSummary resolveNextStep(DashboardOverviewVO overview) {
        DashboardOverviewVO.ResumeSummary resumeSummary = overview.getResumeSummary();
        if (resumeSummary != null && (resumeSummary.getResumeCount() == null || resumeSummary.getResumeCount() == 0)) {
            return DashboardOverviewVO.NextStepSummary.builder()
                    .title("先上传一份简历")
                    .description("把简历整理好后，再继续项目追问、模拟面试和投递会更顺。")
                    .actionPath("/resume")
                    .build();
        }

        DashboardOverviewVO.PlanSummary planSummary = overview.getPlanSummary();
        if (planSummary != null && planSummary.getPlanId() == null) {
            return DashboardOverviewVO.NextStepSummary.builder()
                    .title("先生成一份训练计划")
                    .description("把接下来几天要练的题目、问答和面试排好，再逐项推进。")
                    .actionPath("/study-plan")
                    .build();
        }

        if (planSummary != null && planSummary.getTodayTaskCount() != null && planSummary.getTodayTaskCount() > 0) {
            return DashboardOverviewVO.NextStepSummary.builder()
                    .title("先完成今天的计划")
                    .description(String.format("今天还有 %d 项任务待处理，先清掉这些任务再决定下一步。", planSummary.getTodayTaskCount()))
                    .actionPath("/study-plan")
                    .build();
        }

        DashboardOverviewVO.ApplicationSummary applicationSummary = overview.getApplicationSummary();
        if (applicationSummary != null && applicationSummary.getActiveCount() != null && applicationSummary.getActiveCount() > 0) {
            return DashboardOverviewVO.NextStepSummary.builder()
                    .title("继续推进正在进行的投递")
                    .description(String.format("当前还有 %d 条投递在推进中，先更新进度或补一条面试记录。", applicationSummary.getActiveCount()))
                    .actionPath("/applications")
                    .build();
        }

        if (overview.getWeakPoints() != null && !overview.getWeakPoints().isEmpty()) {
            String categoryName = overview.getWeakPoints().get(0).getCategoryName();
            return DashboardOverviewVO.NextStepSummary.builder()
                    .title(String.format("先补 %s", categoryName))
                    .description("先从最薄弱的题型开始，再去问答页和模拟面试里把回答练顺。")
                    .actionPath("/question")
                    .build();
        }

        return DashboardOverviewVO.NextStepSummary.builder()
                .title("开始一场新的模拟面试")
                .description("用一次完整演练检查最近这段时间的训练有没有真正转成表达能力。")
                .actionPath("/interview")
                .build();
    }
}
