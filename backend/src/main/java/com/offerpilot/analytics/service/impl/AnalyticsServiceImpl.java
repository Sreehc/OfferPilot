package com.offerpilot.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.ProfileTopicRetrospectiveVO;
import com.offerpilot.analytics.vo.TrendVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.dashboard.dto.NextActionVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.entity.InterviewRecord;
import com.offerpilot.interview.entity.InterviewSession;
import com.offerpilot.interview.mapper.InterviewRecordMapper;
import com.offerpilot.interview.mapper.InterviewSessionMapper;
import com.offerpilot.plan.entity.StudyPlan;
import com.offerpilot.plan.entity.StudyPlanTask;
import com.offerpilot.plan.mapper.StudyPlanMapper;
import com.offerpilot.plan.mapper.StudyPlanTaskMapper;
import com.offerpilot.question.entity.Question;
import com.offerpilot.question.mapper.QuestionMapper;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.wrong.entity.ReviewLog;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import com.offerpilot.wrong.support.ReviewSchedulingRules;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private final InterviewRecordMapper recordMapper;
    private final InterviewSessionMapper sessionMapper;
    private final QuestionMapper questionMapper;
    private final CategoryService categoryService;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final StudyPlanTaskMapper studyPlanTaskMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final DashboardService dashboardService;
    private final AdaptiveService adaptiveService;

    @Override
    @Transactional(readOnly = true)
    public TrendVO getAbilityTrend(Long userId, int weeks, List<Long> categoryIds) {
        int safeWeeks = weeks <= 0 ? 12 : weeks;
        LocalDate startDate = LocalDate.now().minusWeeks(safeWeeks).with(DayOfWeek.MONDAY);

        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId)
                .select(WrongQuestion::getNextReviewDate, WrongQuestion::getLastReviewTime, WrongQuestion::getMasteryLevel, WrongQuestion::getEaseFactor, WrongQuestion::getStreak));

        List<TrendVO.MemoryTrendPoint> reviewActivityTrend = buildReviewActivityTrend(userId, safeWeeks);
        List<TrendVO.MemoryTrendPoint> reviewDebtTrend = buildReviewDebtTrend(wrongs, startDate);
        List<TrendVO.MemoryTrendPoint> masteredGrowthTrend = buildMasteryGrowthTrend(wrongs, startDate);

        TrendData interviewTrend = buildInterviewTrend(userId, safeWeeks, categoryIds);
        List<String> weeksAxis = reviewActivityTrend.stream().map(TrendVO.MemoryTrendPoint::getWeek).toList();
        if (weeksAxis.isEmpty()) {
            weeksAxis = interviewTrend.weeks();
        }

        return TrendVO.builder()
                .weeks(weeksAxis)
                .reviewActivityTrend(reviewActivityTrend)
                .reviewDebtTrend(reviewDebtTrend)
                .masteredGrowthTrend(masteredGrowthTrend)
                .overallTrend(interviewTrend.overallTrend())
                .categoryTrends(interviewTrend.categoryTrends())
                .planProgressTrend(buildPlanProgressTrend(userId, safeWeeks))
                .applicationActivityTrend(buildApplicationActivityTrend(userId, safeWeeks))
                .resumeActivityTrend(buildResumeActivityTrend(userId, safeWeeks))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EfficiencyVO getEfficiencyData(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>().eq(WrongQuestion::getUserId, userId));
        List<ReviewLog> reviewLogs = reviewLogMapper.selectList(
                new LambdaQueryWrapper<ReviewLog>()
                        .eq(ReviewLog::getUserId, userId)
                        .orderByAsc(ReviewLog::getCreateTime));

        Map<String, Long> masteryDist = buildMasteryDistribution(wrongs);
        BigDecimal avgEaseFactor = resolveAvgEaseFactor(wrongs);
        Map<Integer, Long> ratingDist = buildRatingDistribution(reviewLogs);
        List<MergedLogPoint> mergedLogs = mergeLogs(userId, reviewLogs);

        List<EfficiencyVO.WeeklyEF> efTrend = buildEfTrend(mergedLogs);
        List<EfficiencyVO.WeeklyForgettingRate> frTrend = buildForgettingRateTrend(mergedLogs);
        List<EfficiencyVO.DebtTrendPoint> reviewDebtTrend = buildDebtTrendPoints(wrongs);
        List<EfficiencyVO.MasteredGrowthPoint> masteredGrowthTrend = buildMasteredGrowthPoints(wrongs);
        Map<String, Long> contentTypeDistribution = mergedLogs.stream()
                .collect(Collectors.groupingBy(MergedLogPoint::contentType, Collectors.counting()));
        List<EfficiencyVO.CategoryMastery> categoryMastery = buildCategoryMastery(userId, wrongs);

        BigDecimal forgettingRate = frTrend.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(frTrend.get(frTrend.size() - 1).getForgettingRate() * 100).setScale(2, RoundingMode.HALF_UP);

        return EfficiencyVO.builder()
                .avgEaseFactor(avgEaseFactor)
                .efTrend(efTrend)
                .ratingDistribution(ratingDist)
                .forgettingRateTrend(frTrend)
                .reviewDebtTrend(reviewDebtTrend)
                .masteredGrowthTrend(masteredGrowthTrend)
                .masteryDistribution(masteryDist)
                .contentTypeDistribution(contentTypeDistribution)
                .categoryMastery(categoryMastery)
                .totalReviews(mergedLogs.size())
                .currentStreak(resolveCurrentStreak(mergedLogs))
                .forgettingRate(forgettingRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LearningInsightsVO getLearningInsights(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thisMonday = today.with(DayOfWeek.MONDAY);
        LocalDate lastMonday = thisMonday.minusWeeks(1);

        List<InterviewSession> recentSessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .isNotNull(InterviewSession::getTotalScore)
                        .ge(InterviewSession::getCreateTime, lastMonday.atStartOfDay())
                        .orderByAsc(InterviewSession::getCreateTime));

        BigDecimal thisWeekAvg = BigDecimal.ZERO;
        BigDecimal lastWeekAvg = BigDecimal.ZERO;
        int thisWeekCount = 0;
        int lastWeekCount = 0;

        for (InterviewSession session : recentSessions) {
            if (session.getStartTime() == null || session.getTotalScore() == null) {
                continue;
            }
            LocalDate sessionDate = session.getStartTime().toLocalDate();
            if (!sessionDate.isBefore(thisMonday)) {
                thisWeekAvg = thisWeekAvg.add(session.getTotalScore());
                thisWeekCount++;
            } else if (!sessionDate.isBefore(lastMonday)) {
                lastWeekAvg = lastWeekAvg.add(session.getTotalScore());
                lastWeekCount++;
            }
        }

        BigDecimal thisAvg = thisWeekCount > 0
                ? thisWeekAvg.divide(BigDecimal.valueOf(thisWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal lastAvg = lastWeekCount > 0
                ? lastWeekAvg.divide(BigDecimal.valueOf(lastWeekCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<LearningInsightsVO.CategoryChange> categoryChanges = buildCategoryChanges(recentSessions, thisMonday);
        List<LearningInsightsVO.HourDistribution> bestStudyHours = analyzeBestHours(userId);

        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId));
        List<ReviewLog> recentReviewLogs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, today.minusDays(7).atStartOfDay())
                .orderByAsc(ReviewLog::getCreateTime));

        StudyPlan activePlan = studyPlanMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .orderByDesc(StudyPlan::getUpdateTime)
                .last("LIMIT 1"));
        int todayPlanCompletedTaskCount = 0;
        int todayPlanTaskCount = 0;
        if (activePlan != null) {
            List<StudyPlanTask> todayTasks = studyPlanTaskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                    .eq(StudyPlanTask::getPlanId, activePlan.getId())
                    .eq(StudyPlanTask::getDayIndex, activePlan.getCurrentDay()));
            todayPlanTaskCount = todayTasks.size();
            todayPlanCompletedTaskCount = (int) todayTasks.stream().filter(task -> "completed".equals(task.getStatus())).count();
        }

        List<JobApplication> applications = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId));
        int activeApplicationCount = (int) applications.stream()
                .filter(item -> List.of("applied", "written", "interview").contains(item.getStatus()))
                .count();
        int offerCount = (int) applications.stream()
                .filter(item -> "offer".equals(item.getStatus()))
                .count();

        List<ResumeFile> resumes = resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getUpdateTime));
        ResumeFile latestResume = resumes.isEmpty() ? null : resumes.get(0);

        NextActionVO nextAction = dashboardService.overview().getNextAction();

        return LearningInsightsVO.builder()
                .thisWeekAvgScore(thisAvg)
                .lastWeekAvgScore(lastAvg)
                .thisWeekInterviewCount(thisWeekCount)
                .lastWeekInterviewCount(lastWeekCount)
                .todayCompletionStatus(resolveTodayCompletionStatus(recentReviewLogs))
                .reviewDebtStatus(resolveReviewDebtStatus(wrongs))
                .masteryGrowthStatus(resolveMasteryGrowthStatus(wrongs))
                .planExecutionStatus(resolvePlanExecutionStatus(activePlan, todayPlanCompletedTaskCount, todayPlanTaskCount))
                .todayPlanCompletedTaskCount(todayPlanCompletedTaskCount)
                .todayPlanTaskCount(todayPlanTaskCount)
                .activePlanProgressRate(activePlan == null ? BigDecimal.ZERO : defaultDecimal(activePlan.getProgressRate()))
                .activePlanTitle(activePlan == null ? null : activePlan.getTitle())
                .applicationActiveCount(activeApplicationCount)
                .applicationOfferCount(offerCount)
                .applicationStatus(resolveApplicationStatus(applications, activeApplicationCount, offerCount))
                .resumeCount(resumes.size())
                .latestResumeTitle(latestResume == null ? null : latestResume.getTitle())
                .resumeReadinessStatus(resolveResumeReadinessStatus(resumes, latestResume))
                .interviewConversionStatus(resolveInterviewConversionStatus(thisWeekCount, activeApplicationCount, offerCount))
                .nextAction(nextAction)
                .categoryChanges(categoryChanges)
                .bestStudyHours(bestStudyHours)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AbilityProfileVO getAbilityProfile(Long userId) {
        return adaptiveService.getAbilityProfile(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileTopicDetailVO getProfileTopicDetail(Long userId, Long categoryId) {
        AbilityProfileVO profile = adaptiveService.getAbilityProfile(userId);
        CategoryAbilityVO categoryAbility = profile.getCategoryAbilities().stream()
                .filter(item -> Objects.equals(item.getCategoryId(), categoryId))
                .findFirst()
                .orElseThrow(() -> new com.offerpilot.common.exception.BusinessException(
                        com.offerpilot.common.api.ResultCode.NOT_FOUND.getCode(), "topic not found"));

        EfficiencyVO efficiency = getEfficiencyData(userId);
        EfficiencyVO.CategoryMastery categoryMastery = efficiency.getCategoryMastery().stream()
                .filter(item -> Objects.equals(item.getCategoryId(), categoryId))
                .findFirst()
                .orElse(EfficiencyVO.CategoryMastery.builder()
                        .categoryId(categoryId)
                        .categoryName(categoryAbility.getCategoryName())
                        .totalCards(0)
                        .masteredCards(0)
                        .dueCount(0)
                        .masteryRate(BigDecimal.ZERO)
                        .build());

        TrendVO trend = getAbilityTrend(userId, 8, List.of(categoryId));
        List<ProfileTopicDetailVO.WeeklyTopicScore> recentScores = trend.getCategoryTrends().stream()
                .filter(item -> Objects.equals(item.getCategoryId(), categoryId))
                .findFirst()
                .map(item -> item.getPoints().stream()
                        .map(point -> ProfileTopicDetailVO.WeeklyTopicScore.builder()
                                .week(point.getWeek())
                                .score(point.getScore().doubleValue())
                                .build())
                        .toList())
                .orElse(List.of());

        String evidenceStatus = resolveTopicEvidenceStatus(categoryAbility, categoryMastery, recentScores);
        String evidenceSummary = buildTopicEvidenceSummary(categoryAbility, categoryMastery, recentScores, evidenceStatus);
        List<String> focusRecommendations = buildTopicRecommendations(categoryAbility, categoryMastery, recentScores);
        String summary = buildTopicSummary(categoryAbility, categoryMastery, recentScores);

        return ProfileTopicDetailVO.builder()
                .categoryId(categoryAbility.getCategoryId())
                .categoryName(categoryAbility.getCategoryName())
                .abilityScore(categoryAbility.getAbilityScore())
                .interviewCount(categoryAbility.getInterviewCount())
                .recordingReviewCount(categoryAbility.getRecordingReviewCount())
                .jobPrepCount(categoryAbility.getJobPrepCount())
                .copilotPrepCount(categoryAbility.getCopilotPrepCount())
                .copilotRealtimeCount(categoryAbility.getCopilotRealtimeCount())
                .applicationFeedbackCount(categoryAbility.getApplicationFeedbackCount())
                .resumeEvidenceCount(categoryAbility.getResumeEvidenceCount())
                .wrongCount(categoryAbility.getWrongCount())
                .weak(Boolean.TRUE.equals(categoryAbility.getIsWeak()))
                .recommendedDifficulty(categoryAbility.getRecommendedDifficulty())
                .totalCards(categoryMastery.getTotalCards())
                .masteredCards(categoryMastery.getMasteredCards())
                .dueCount(categoryMastery.getDueCount())
                .masteryRate(categoryMastery.getMasteryRate())
                .evidenceStatus(evidenceStatus)
                .evidenceSummary(evidenceSummary)
                .retrospectiveReady(!"insufficient".equals(evidenceStatus))
                .summary(summary)
                .focusRecommendations(focusRecommendations)
                .recentScores(recentScores)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileTopicRetrospectiveVO buildProfileTopicRetrospective(Long userId, Long categoryId) {
        ProfileTopicDetailVO detail = getProfileTopicDetail(userId, categoryId);
        LearningInsightsVO insights = getLearningInsights(userId);
        String stage = resolveRetrospectiveStage(detail);

        List<String> keySignals = buildRetrospectiveSignals(detail, insights);
        List<String> riskSignals = buildRetrospectiveRisks(detail, insights);
        List<String> nextActions = buildRetrospectiveActions(detail, insights);

        return ProfileTopicRetrospectiveVO.builder()
                .categoryId(detail.getCategoryId())
                .categoryName(detail.getCategoryName())
                .title("近 8 周「" + detail.getCategoryName() + "」领域回顾")
                .stage(stage)
                .evidenceStatus(detail.getEvidenceStatus())
                .evidenceSummary(detail.getEvidenceSummary())
                .summary(buildRetrospectiveSummary(detail, stage, riskSignals))
                .keySignals(keySignals)
                .riskSignals(riskSignals)
                .nextActions(nextActions)
                .build();
    }

    private List<TrendVO.PlanTrendPoint> buildPlanProgressTrend(Long userId, int weeks) {
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);
        List<StudyPlan> plans = studyPlanMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .ge(StudyPlan::getUpdateTime, startDate.atStartOfDay())
                .orderByAsc(StudyPlan::getUpdateTime));
        Map<String, List<StudyPlan>> byWeek = plans.stream()
                .filter(plan -> plan.getUpdateTime() != null)
                .collect(Collectors.groupingBy(plan -> formatWeek(plan.getUpdateTime().toLocalDate()), LinkedHashMap::new, Collectors.toList()));
        return byWeek.entrySet().stream()
                .map(entry -> TrendVO.PlanTrendPoint.builder()
                        .week(entry.getKey())
                        .progressRate(avg(entry.getValue().stream().map(StudyPlan::getProgressRate).filter(Objects::nonNull).toList()))
                        .completedTaskCount(entry.getValue().stream().mapToInt(plan -> plan.getCompletedTaskCount() == null ? 0 : plan.getCompletedTaskCount()).sum())
                        .totalTaskCount(entry.getValue().stream().mapToInt(plan -> plan.getTotalTaskCount() == null ? 0 : plan.getTotalTaskCount()).sum())
                        .build())
                .toList();
    }

    private String resolveRetrospectiveStage(ProfileTopicDetailVO detail) {
        if (Boolean.TRUE.equals(detail.getWeak()) || detail.getDueCount() >= 6 || detail.getAbilityScore() < 60) {
            return "needs_attention";
        }
        if (detail.getMasteryRate().doubleValue() >= 70 && detail.getAbilityScore() >= 75) {
            return "stable";
        }
        return "building";
    }

    private String buildRetrospectiveSummary(ProfileTopicDetailVO detail, String stage, List<String> riskSignals) {
        if ("insufficient".equals(detail.getEvidenceStatus())) {
            return "「" + detail.getCategoryName() + "」当前证据还不够完整，这份领域回顾会优先基于已有训练记录给出阶段性建议。";
        }
        String stageText = switch (stage) {
            case "stable" -> "已经进入相对稳定区间";
            case "needs_attention" -> "当前仍是需要优先处理的薄弱领域";
            default -> "正在从理解阶段向稳定输出阶段过渡";
        };
        if (riskSignals.isEmpty()) {
            return "「" + detail.getCategoryName() + "」" + stageText + "，建议继续把当前口径固化成固定题型的答法。";
        }
        return "「" + detail.getCategoryName() + "」" + stageText + "，但还存在 "
                + abbreviateJoined(riskSignals, 2)
                + " 等风险，需要继续安排下一轮针对性训练。";
    }

    private List<String> buildRetrospectiveSignals(ProfileTopicDetailVO detail, LearningInsightsVO insights) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        signals.add("当前画像分约为 " + Math.round(detail.getAbilityScore()) + "，掌握率约为 "
                + detail.getMasteryRate().setScale(0, RoundingMode.HALF_UP) + "%。");
        signals.add("最近共累计 " + detail.getInterviewCount() + " 场相关模拟面试、" + detail.getWrongCount() + " 道相关错题。");
        if (detail.getRecordingReviewCount() != null && detail.getRecordingReviewCount() > 0) {
            signals.add("另外已经沉淀 " + detail.getRecordingReviewCount() + " 次相关真实录音复盘证据。");
        }
        if (safeInt(detail.getJobPrepCount()) > 0 || safeInt(detail.getCopilotPrepCount()) > 0) {
            signals.add("同时已有 " + safeInt(detail.getJobPrepCount()) + " 次 JD 备面、"
                    + safeInt(detail.getCopilotPrepCount()) + " 次 Copilot Prep 证据写回长期画像。");
        }
        if (safeInt(detail.getApplicationFeedbackCount()) > 0 || safeInt(detail.getResumeEvidenceCount()) > 0) {
            signals.add("另外已有 " + safeInt(detail.getApplicationFeedbackCount()) + " 条相关投递反馈证据、"
                    + safeInt(detail.getResumeEvidenceCount()) + " 份相关简历表达证据写回长期画像。");
        }
        if (!detail.getRecentScores().isEmpty()) {
            double latest = detail.getRecentScores().get(detail.getRecentScores().size() - 1).getScore();
            double first = detail.getRecentScores().get(0).getScore();
            String trendText = latest >= first ? "最近趋势整体上行" : "最近趋势仍有回落";
            signals.add(trendText + "，区间变化约 " + Math.round(first) + " -> " + Math.round(latest) + "。");
        }
        if (StringUtils.hasText(insights.getPlanExecutionStatus())) {
            signals.add("当前主线计划状态：" + insights.getPlanExecutionStatus());
        }
        return signals.stream().limit(4).toList();
    }

    private List<String> buildRetrospectiveRisks(ProfileTopicDetailVO detail, LearningInsightsVO insights) {
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        if ((detail.getRecordingReviewCount() == null || detail.getRecordingReviewCount() == 0)
                && detail.getAbilityScore() < 70) {
            risks.add("当前还缺真实录音复盘证据，表达层问题可能还没被完整暴露。");
        }
        if (safeInt(detail.getJobPrepCount()) == 0 && safeInt(detail.getCopilotPrepCount()) == 0
                && detail.getAbilityScore() < 75) {
            risks.add("当前还缺岗位化 Prep 证据，主题能力还没完整映射到真实 JD 和实战场景。");
        }
        if (safeInt(detail.getApplicationFeedbackCount()) == 0 && detail.getAbilityScore() < 75) {
            risks.add("当前还缺真实投递反馈证据，这个主题是否真能支撑岗位推进还没被验证。");
        }
        if (safeInt(detail.getResumeEvidenceCount()) == 0 && detail.getAbilityScore() < 75) {
            risks.add("当前还缺简历项目表达证据，这个主题在自我介绍和项目叙述里还没被验证。");
        }
        if (detail.getDueCount() > 0) {
            risks.add("还有 " + detail.getDueCount() + " 个待复盘点没有清完，容易让掌握度回落。");
        }
        if (detail.getWrongCount() >= detail.getInterviewCount() && detail.getWrongCount() > 0) {
            risks.add("错题沉积速度仍高于面试验证速度，说明理解还没完全转成稳定表达。");
        }
        if (detail.getAbilityScore() < 65) {
            risks.add("画像分仍处在较低区间，继续追问时容易暴露原理或案例深度不够。");
        }
        if (StringUtils.hasText(insights.getReviewDebtStatus()) && !"已清到期待复盘".equals(insights.getReviewDebtStatus())) {
            risks.add("整体复盘债务还没清完，会拖慢这个领域的稳定率提升。");
        }
        return risks.stream().limit(4).toList();
    }

    private List<String> buildRetrospectiveActions(ProfileTopicDetailVO detail, LearningInsightsVO insights) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        actions.add("先围绕「" + detail.getCategoryName() + "」安排 1 轮专项题库或知识库追问，收紧核心口径。");
        if (detail.getRecordingReviewCount() == null || detail.getRecordingReviewCount() == 0) {
            actions.add("补 1 次真实录音复盘，把表达层薄弱点正式写回画像。");
        }
        if (safeInt(detail.getJobPrepCount()) == 0) {
            actions.add("补 1 次 JD 定向备面，把这个主题压到目标岗位语境下重新收紧。");
        }
        if (safeInt(detail.getApplicationFeedbackCount()) == 0 && safeInt(detail.getResumeEvidenceCount()) == 0) {
            actions.add("把这个主题同步到至少 1 条真实投递推进反馈里，并补 1 轮简历项目表达或自我介绍打磨，验证它是否真的能支撑岗位转化。");
        } else if (safeInt(detail.getApplicationFeedbackCount()) == 0) {
            actions.add("把这个主题同步到至少 1 条真实投递推进反馈里，验证它是否真的能支撑岗位转化。");
        } else if (safeInt(detail.getResumeEvidenceCount()) == 0) {
            actions.add("补 1 轮简历项目表达或自我介绍打磨，把这个主题写进真实面试口径。");
        }
        if (safeInt(detail.getCopilotPrepCount()) == 0) {
            actions.add("在下一次正式面试前补 1 次 Copilot Prep，提前暴露实时追问风险。");
        }
        if (detail.getDueCount() > 0) {
            actions.add("优先清理到期待复盘的题和片段，避免旧薄弱点继续堆积。");
        }
        actions.add("从建议动作里挑 1 个点，补成 90 秒口语答案，再用一场模拟面试验证。");
        if (insights.getApplicationActiveCount() != null && insights.getApplicationActiveCount() > 0) {
            actions.add("把这块领域的表达同步到下一批投递和 JD 备面里，不要只停留在训练页。");
        }
        return actions.stream().limit(4).toList();
    }

    private String abbreviateJoined(List<String> items, int limit) {
        return items.stream().limit(limit).map(item -> item.replace("。", "")).collect(Collectors.joining("、"));
    }

    private List<String> buildTopicRecommendations(CategoryAbilityVO categoryAbility,
                                                   EfficiencyVO.CategoryMastery categoryMastery,
                                                   List<ProfileTopicDetailVO.WeeklyTopicScore> recentScores) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        String evidenceStatus = resolveTopicEvidenceStatus(categoryAbility, categoryMastery, recentScores);
        if ("insufficient".equals(evidenceStatus)) {
            suggestions.add("当前领域证据还不够完整，先补 1 次定向模拟或录音复盘，再看长期画像变化。");
        } else if ("forming".equals(evidenceStatus)) {
            suggestions.add("当前领域画像还在形成中，建议继续补 1-2 次同主题训练，把趋势拉实。");
        }
        if (Boolean.TRUE.equals(categoryAbility.getIsWeak())) {
            suggestions.add("先围绕这个主题做一轮基础巩固，再回到高强度模拟。");
        }
        if (safeInt(categoryAbility.getApplicationFeedbackCount()) > 0) {
            suggestions.add("把已出现的投递反馈缺口同步回训练和简历表达，避免同类问题在下一批岗位重复出现。");
        } else {
            suggestions.add("当前还缺真实投递反馈，建议把这个主题同步到至少 1 条在投岗位里验证。");
        }
        if (safeInt(categoryAbility.getResumeEvidenceCount()) > 0) {
            suggestions.add("把简历里的项目表达继续对齐训练口径，确保这个主题能直接讲成案例。");
        } else {
            suggestions.add("当前还缺简历项目表达证据，建议先补项目案例和自我介绍口径。");
        }
        if (categoryAbility.getRecordingReviewCount() != null && categoryAbility.getRecordingReviewCount() > 0) {
            suggestions.add("把真实录音复盘里暴露的表达问题同步进下一轮模拟，避免只修知识点不修表达。");
        } else {
            suggestions.add("当前还缺真实录音复盘证据，建议补 1 次录音回听验证表达层问题。");
        }
        if (safeInt(categoryAbility.getJobPrepCount()) > 0 || safeInt(categoryAbility.getCopilotPrepCount()) > 0) {
            suggestions.add("把已有 JD 备面和 Copilot Prep 里的岗位化表达直接带进下一轮模拟，不要重新起草。");
        } else {
            suggestions.add("当前还没把这个主题压到真实岗位语境，建议补 1 次 JD 备面或 Copilot Prep。");
        }
        if (safeInt(categoryAbility.getCopilotRealtimeCount()) > 0) {
            suggestions.add("已经有实时 Copilot 证据，优先复盘真实追问里的卡点和临场表达波动。");
        } else {
            suggestions.add("当前还没进入实时 Copilot 阶段，建议至少做 1 次实时追问演练验证临场稳定性。");
        }
        if (categoryMastery.getDueCount() > 0) {
            suggestions.add("当前有 " + categoryMastery.getDueCount() + " 道题待复盘，先清掉到期负债。");
        }
        if (categoryAbility.getWrongCount() != null && categoryAbility.getWrongCount() > 0) {
            suggestions.add("把错题里重复出现的追问方式整理成标准答题骨架。");
        }
        if (recentScores.size() >= 2) {
            Double last = recentScores.get(recentScores.size() - 1).getScore();
            Double previous = recentScores.get(recentScores.size() - 2).getScore();
            if (last != null && previous != null && last < previous) {
                suggestions.add("最近一周分数回落，建议先做 1 次定向模拟，再安排复盘。");
            }
        }
        suggestions.add("下一轮训练优先围绕「" + categoryAbility.getCategoryName() + "」准备项目案例、原理解释和追问取舍。");
        return suggestions.stream().limit(4).toList();
    }

    private String buildTopicSummary(CategoryAbilityVO categoryAbility,
                                     EfficiencyVO.CategoryMastery categoryMastery,
                                     List<ProfileTopicDetailVO.WeeklyTopicScore> recentScores) {
        String abilityText = "当前主题画像分数约 " + Math.round(categoryAbility.getAbilityScore()) + " 分";
        String reviewText = categoryMastery.getDueCount() > 0
                ? "，并且还有 " + categoryMastery.getDueCount() + " 道待复盘题"
                : "，当前没有待复盘负债";
        String recordingText = categoryAbility.getRecordingReviewCount() != null && categoryAbility.getRecordingReviewCount() > 0
                ? "，已经累积 " + categoryAbility.getRecordingReviewCount() + " 次真实录音复盘证据"
                : "，但还缺真实录音复盘证据";
        String prepText = safeInt(categoryAbility.getJobPrepCount()) > 0 || safeInt(categoryAbility.getCopilotPrepCount()) > 0
                ? "，并且已有 " + safeInt(categoryAbility.getJobPrepCount()) + " 次 JD 备面、"
                + safeInt(categoryAbility.getCopilotPrepCount()) + " 次 Copilot Prep 证据"
                : "，当前还没沉淀岗位化 Prep 证据";
        String realtimeText = safeInt(categoryAbility.getCopilotRealtimeCount()) > 0
                ? "，并且已有 " + safeInt(categoryAbility.getCopilotRealtimeCount()) + " 次实时 Copilot 证据"
                : "，当前还缺实时 Copilot 阶段证据";
        String applicationText = safeInt(categoryAbility.getApplicationFeedbackCount()) > 0
                ? "，同时已有 " + safeInt(categoryAbility.getApplicationFeedbackCount()) + " 条投递反馈证据"
                : "，但还缺真实投递反馈证据";
        String resumeText = safeInt(categoryAbility.getResumeEvidenceCount()) > 0
                ? "，并且已有 " + safeInt(categoryAbility.getResumeEvidenceCount()) + " 份简历表达证据"
                : "，当前还缺简历项目表达证据";
        String trendText = "";
        if (recentScores.size() >= 2) {
            Double first = recentScores.get(0).getScore();
            Double last = recentScores.get(recentScores.size() - 1).getScore();
            if (first != null && last != null) {
                trendText = last >= first ? "，近几周趋势在回升" : "，近几周趋势有回落";
            }
        }
        String evidenceText = switch (resolveTopicEvidenceStatus(categoryAbility, categoryMastery, recentScores)) {
            case "insufficient" -> "，当前证据还不够完整";
            case "forming" -> "，当前画像仍在形成中";
            default -> "";
        };
        return "主题「" + categoryAbility.getCategoryName() + "」" + abilityText + reviewText + recordingText + prepText + realtimeText + applicationText + resumeText + trendText + evidenceText + "。";
    }

    private String resolveTopicEvidenceStatus(CategoryAbilityVO categoryAbility,
                                              EfficiencyVO.CategoryMastery categoryMastery,
                                              List<ProfileTopicDetailVO.WeeklyTopicScore> recentScores) {
        int evidenceUnits = safeInt(categoryAbility.getInterviewCount())
                + safeInt(categoryAbility.getRecordingReviewCount())
                + safeInt(categoryAbility.getJobPrepCount())
                + safeInt(categoryAbility.getCopilotPrepCount())
                + safeInt(categoryAbility.getCopilotRealtimeCount())
                + safeInt(categoryAbility.getApplicationFeedbackCount())
                + safeInt(categoryAbility.getResumeEvidenceCount())
                + (safeInt(categoryAbility.getWrongCount()) > 0 ? 1 : 0)
                + (safeInt(categoryMastery.getDueCount()) > 0 ? 1 : 0);
        if (evidenceUnits <= 1 && recentScores.size() <= 1) {
            return "insufficient";
        }
        if (evidenceUnits <= 3 || recentScores.size() <= 1) {
            return "forming";
        }
        return "ready";
    }

    private String buildTopicEvidenceSummary(CategoryAbilityVO categoryAbility,
                                             EfficiencyVO.CategoryMastery categoryMastery,
                                             List<ProfileTopicDetailVO.WeeklyTopicScore> recentScores,
                                             String evidenceStatus) {
        int interviews = safeInt(categoryAbility.getInterviewCount());
        int recordingReviews = safeInt(categoryAbility.getRecordingReviewCount());
        int jobPrepCount = safeInt(categoryAbility.getJobPrepCount());
        int copilotPrepCount = safeInt(categoryAbility.getCopilotPrepCount());
        int copilotRealtimeCount = safeInt(categoryAbility.getCopilotRealtimeCount());
        int applicationFeedbackCount = safeInt(categoryAbility.getApplicationFeedbackCount());
        int resumeEvidenceCount = safeInt(categoryAbility.getResumeEvidenceCount());
        int wrongCount = safeInt(categoryAbility.getWrongCount());
        int dueCount = safeInt(categoryMastery.getDueCount());
        return switch (evidenceStatus) {
            case "insufficient" -> "当前只沉淀了少量训练证据："
                    + interviews + " 场模拟面试、"
                    + recordingReviews + " 次录音复盘、"
                    + jobPrepCount + " 次 JD 备面、"
                    + copilotPrepCount + " 次 Copilot Prep、"
                    + copilotRealtimeCount + " 次实时 Copilot、"
                    + applicationFeedbackCount + " 条投递反馈、"
                    + resumeEvidenceCount + " 份简历表达、"
                    + wrongCount + " 道相关错题。建议先补 1-2 次同主题训练。";
            case "forming" -> "当前领域画像还在形成中，已沉淀 "
                    + interviews + " 场模拟面试、"
                    + recordingReviews + " 次录音复盘、"
                    + jobPrepCount + " 次 JD 备面、"
                    + copilotPrepCount + " 次 Copilot Prep、"
                    + copilotRealtimeCount + " 次实时 Copilot、"
                    + applicationFeedbackCount + " 条投递反馈、"
                    + resumeEvidenceCount + " 份简历表达、"
                    + wrongCount + " 道相关错题"
                    + (dueCount > 0 ? "，以及 " + dueCount + " 个待复盘点" : "")
                    + "。";
            default -> "当前领域证据已经比较完整，最近趋势点 "
                    + recentScores.size() + " 个，支持继续做稳定性回顾和下一轮动作规划。";
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<TrendVO.ApplicationTrendPoint> buildApplicationActivityTrend(Long userId, int weeks) {
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);
        List<JobApplication> applications = jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .ge(JobApplication::getCreateTime, startDate.atStartOfDay())
                .orderByAsc(JobApplication::getCreateTime));
        Map<String, List<JobApplication>> byWeek = applications.stream()
                .filter(item -> item.getCreateTime() != null)
                .collect(Collectors.groupingBy(item -> formatWeek(item.getCreateTime().toLocalDate()), LinkedHashMap::new, Collectors.toList()));
        return byWeek.entrySet().stream()
                .map(entry -> TrendVO.ApplicationTrendPoint.builder()
                        .week(entry.getKey())
                        .totalCount(entry.getValue().size())
                        .activeCount((int) entry.getValue().stream().filter(item -> List.of("applied", "written", "interview").contains(item.getStatus())).count())
                        .interviewCount((int) entry.getValue().stream().filter(item -> "interview".equals(item.getStatus())).count())
                        .offerCount((int) entry.getValue().stream().filter(item -> "offer".equals(item.getStatus())).count())
                        .build())
                .toList();
    }

    private List<TrendVO.ResumeTrendPoint> buildResumeActivityTrend(Long userId, int weeks) {
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);
        List<ResumeFile> resumes = resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .ge(ResumeFile::getCreateTime, startDate.atStartOfDay())
                .orderByAsc(ResumeFile::getCreateTime));
        Map<String, List<ResumeFile>> byWeek = resumes.stream()
                .filter(item -> item.getCreateTime() != null)
                .collect(Collectors.groupingBy(item -> formatWeek(item.getCreateTime().toLocalDate()), LinkedHashMap::new, Collectors.toList()));
        return byWeek.entrySet().stream()
                .map(entry -> TrendVO.ResumeTrendPoint.builder()
                        .week(entry.getKey())
                        .uploadCount(entry.getValue().size())
                        .parsedCount((int) entry.getValue().stream().filter(item -> "parsed".equals(item.getParseStatus())).count())
                        .build())
                .toList();
    }

    private TrendData buildInterviewTrend(Long userId, int weeks, List<Long> categoryIds) {
        LocalDate startDate = LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY);
        List<InterviewRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .eq(InterviewRecord::getUserId, userId)
                        .isNotNull(InterviewRecord::getScore)
                        .ge(InterviewRecord::getCreateTime, startDate.atStartOfDay()));
        if (records.isEmpty()) {
            return new TrendData(List.of(), List.of(), List.of());
        }

        Set<Long> sessionIds = records.stream().map(InterviewRecord::getSessionId).collect(Collectors.toSet());
        Map<Long, InterviewSession> sessionMap = sessionMapper.selectBatchIds(sessionIds)
                .stream()
                .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));
        Set<Long> questionIds = records.stream().map(InterviewRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> filterCategoryIds = categoryIds == null || categoryIds.isEmpty() ? null : Set.copyOf(categoryIds);

        Map<String, List<InterviewRecord>> byWeek = new LinkedHashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            if (session == null || session.getStartTime() == null) {
                continue;
            }
            String weekKey = formatWeek(session.getStartTime().toLocalDate());
            byWeek.computeIfAbsent(weekKey, key -> new ArrayList<>()).add(record);
        }

        List<TrendVO.WeeklyPoint> overallTrend = new ArrayList<>();
        for (Map.Entry<String, List<InterviewRecord>> entry : byWeek.entrySet()) {
            List<BigDecimal> scores = entry.getValue().stream()
                    .map(InterviewRecord::getScore)
                    .filter(Objects::nonNull)
                    .toList();
            overallTrend.add(TrendVO.WeeklyPoint.builder()
                    .week(entry.getKey())
                    .score(avg(scores))
                    .count(scores.size())
                    .build());
        }

        Map<Long, String> categoryNameMap = new HashMap<>();
        Set<Long> allCategoryIds = records.stream()
                .map(record -> questionMap.get(record.getQuestionId()))
                .filter(Objects::nonNull)
                .map(Question::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!allCategoryIds.isEmpty()) {
            categoryService.listByIds(allCategoryIds).forEach(category -> categoryNameMap.put(category.getId(), category.getName()));
        }

        Map<Long, Map<String, List<BigDecimal>>> categoryWeekScores = new HashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            Question question = questionMap.get(record.getQuestionId());
            if (session == null || question == null || question.getCategoryId() == null || session.getStartTime() == null) {
                continue;
            }
            if (filterCategoryIds != null && !filterCategoryIds.contains(question.getCategoryId())) {
                continue;
            }
            String weekKey = formatWeek(session.getStartTime().toLocalDate());
            categoryWeekScores
                    .computeIfAbsent(question.getCategoryId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(weekKey, key -> new ArrayList<>())
                    .add(record.getScore());
        }

        List<TrendVO.CategoryTrend> categoryTrends = new ArrayList<>();
        for (Map.Entry<Long, Map<String, List<BigDecimal>>> entry : categoryWeekScores.entrySet()) {
            List<TrendVO.WeeklyPoint> points = entry.getValue().entrySet().stream()
                    .map(weekEntry -> TrendVO.WeeklyPoint.builder()
                            .week(weekEntry.getKey())
                            .score(avg(weekEntry.getValue()))
                            .count(weekEntry.getValue().size())
                            .build())
                    .toList();
            categoryTrends.add(TrendVO.CategoryTrend.builder()
                    .categoryId(entry.getKey())
                    .categoryName(categoryNameMap.getOrDefault(entry.getKey(), "未分类"))
                    .points(points)
                    .build());
        }

        List<String> allWeeks = new ArrayList<>(byWeek.keySet());
        allWeeks.sort(String::compareTo);
        return new TrendData(allWeeks, overallTrend, categoryTrends);
    }

    private List<TrendVO.MemoryTrendPoint> buildReviewActivityTrend(Long userId, int weeks) {
        List<ReviewLog> logs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .ge(ReviewLog::getCreateTime, LocalDate.now().minusWeeks(weeks).with(DayOfWeek.MONDAY).atStartOfDay())
                .orderByAsc(ReviewLog::getCreateTime));
        Map<String, Long> countByWeek = logs.stream()
                .filter(log -> log.getCreateTime() != null)
                .collect(Collectors.groupingBy(log -> formatWeek(log.getCreateTime().toLocalDate()), LinkedHashMap::new, Collectors.counting()));
        return countByWeek.entrySet().stream()
                .map(entry -> TrendVO.MemoryTrendPoint.builder()
                        .week(entry.getKey())
                        .value(BigDecimal.valueOf(entry.getValue()))
                        .count(entry.getValue().intValue())
                        .build())
                .toList();
    }

    private List<TrendVO.MemoryTrendPoint> buildReviewDebtTrend(List<WrongQuestion> wrongs, LocalDate startDate) {
        Map<String, Integer> debtByWeek = new LinkedHashMap<>();
        for (WrongQuestion wrong : wrongs) {
            LocalDate date = wrong.getNextReviewDate();
            if (date == null || date.isBefore(startDate)) {
                continue;
            }
            String week = formatWeek(date);
            debtByWeek.merge(week, 1, Integer::sum);
        }
        return debtByWeek.entrySet().stream()
                .map(entry -> TrendVO.MemoryTrendPoint.builder()
                        .week(entry.getKey())
                        .value(BigDecimal.valueOf(entry.getValue()))
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private List<TrendVO.MemoryTrendPoint> buildMasteryGrowthTrend(List<WrongQuestion> wrongs, LocalDate startDate) {
        Map<String, Integer> masteryByWeek = new LinkedHashMap<>();
        for (WrongQuestion wrong : wrongs) {
            if (!"mastered".equals(wrong.getMasteryLevel()) || wrong.getLastReviewTime() == null) {
                continue;
            }
            LocalDate date = wrong.getLastReviewTime().toLocalDate();
            if (date.isBefore(startDate)) {
                continue;
            }
            masteryByWeek.merge(formatWeek(date), 1, Integer::sum);
        }
        return masteryByWeek.entrySet().stream()
                .map(entry -> TrendVO.MemoryTrendPoint.builder()
                        .week(entry.getKey())
                        .value(BigDecimal.valueOf(entry.getValue()))
                        .count(entry.getValue())
                        .build())
                .toList();
    }

    private Map<String, Long> buildMasteryDistribution(List<WrongQuestion> wrongs) {
        Map<String, Long> masteryDistribution = new HashMap<>();
        wrongs.forEach(wrong -> masteryDistribution.merge(
                wrong.getMasteryLevel() != null ? wrong.getMasteryLevel() : "not_started",
                1L,
                Long::sum));
        return masteryDistribution;
    }

    private BigDecimal resolveAvgEaseFactor(List<WrongQuestion> wrongs) {
        BigDecimal efSum = wrongs.stream()
                .map(WrongQuestion::getEaseFactor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long efCount = wrongs.stream().filter(wrong -> wrong.getEaseFactor() != null).count();
        return efCount > 0
                ? efSum.divide(BigDecimal.valueOf(efCount), 2, RoundingMode.HALF_UP)
                : DEFAULT_EASE_FACTOR;
    }

    private Map<Integer, Long> buildRatingDistribution(List<ReviewLog> reviewLogs) {
        Map<Integer, Long> ratingDistribution = new HashMap<>();
        reviewLogs.forEach(logItem -> ratingDistribution.merge(logItem.getRating(), 1L, Long::sum));
        return ratingDistribution;
    }

    private List<MergedLogPoint> mergeLogs(Long userId, List<ReviewLog> reviewLogs) {
        List<MergedLogPoint> mergedLogs = new ArrayList<>();
        reviewLogs.forEach(logItem -> mergedLogs.add(new MergedLogPoint(
                formatWeek(logItem.getCreateTime().toLocalDate()),
                logItem.getEaseFactorAfter(),
                logItem.getRating(),
                resolveWrongContentType(userId, logItem.getWrongQuestionId()),
                logItem.getCreateTime())));
        mergedLogs.sort(Comparator.comparing(MergedLogPoint::createTime));
        return mergedLogs;
    }

    private int resolveCurrentStreak(List<MergedLogPoint> mergedLogs) {
        if (mergedLogs.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now();
        Set<LocalDate> reviewDays = mergedLogs.stream().map(log -> log.createTime().toLocalDate()).collect(Collectors.toSet());
        if (!reviewDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (reviewDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private List<EfficiencyVO.WeeklyEF> buildEfTrend(List<MergedLogPoint> mergedLogs) {
        Map<String, List<MergedLogPoint>> logsByWeek = mergedLogs.stream()
                .collect(Collectors.groupingBy(MergedLogPoint::week, LinkedHashMap::new, Collectors.toList()));
        List<EfficiencyVO.WeeklyEF> result = new ArrayList<>();
        for (Map.Entry<String, List<MergedLogPoint>> entry : logsByWeek.entrySet()) {
            BigDecimal lastEf = entry.getValue().stream()
                    .map(MergedLogPoint::easeFactorAfter)
                    .filter(Objects::nonNull)
                    .reduce((first, second) -> second)
                    .orElse(DEFAULT_EASE_FACTOR);
            result.add(EfficiencyVO.WeeklyEF.builder()
                    .week(entry.getKey())
                    .avgEF(lastEf)
                    .reviewCount(entry.getValue().size())
                    .build());
        }
        return result;
    }

    private List<EfficiencyVO.WeeklyForgettingRate> buildForgettingRateTrend(List<MergedLogPoint> mergedLogs) {
        Map<String, List<MergedLogPoint>> logsByWeek = mergedLogs.stream()
                .collect(Collectors.groupingBy(MergedLogPoint::week, LinkedHashMap::new, Collectors.toList()));
        List<EfficiencyVO.WeeklyForgettingRate> result = new ArrayList<>();
        for (Map.Entry<String, List<MergedLogPoint>> entry : logsByWeek.entrySet()) {
            long againCount = entry.getValue().stream().filter(log -> log.rating() == 1).count();
            double forgettingRate = entry.getValue().isEmpty() ? 0.0 : (double) againCount / entry.getValue().size();
            result.add(EfficiencyVO.WeeklyForgettingRate.builder()
                    .week(entry.getKey())
                    .forgettingRate(forgettingRate)
                    .totalRatings(entry.getValue().size())
                    .againCount((int) againCount)
                    .build());
        }
        return result;
    }

    private List<EfficiencyVO.DebtTrendPoint> buildDebtTrendPoints(List<WrongQuestion> wrongs) {
        Map<LocalDate, Integer> dueByDate = new LinkedHashMap<>();
        wrongs.stream()
                .map(WrongQuestion::getNextReviewDate)
                .filter(Objects::nonNull)
                .sorted()
                .forEach(date -> dueByDate.merge(date, 1, Integer::sum));
        return dueByDate.entrySet().stream()
                .map(entry -> EfficiencyVO.DebtTrendPoint.builder()
                        .label(entry.getKey().toString())
                        .reviewDebtCount(entry.getValue())
                        .build())
                .toList();
    }

    private List<EfficiencyVO.MasteredGrowthPoint> buildMasteredGrowthPoints(List<WrongQuestion> wrongs) {
        Map<LocalDate, Integer> masteryByDate = new LinkedHashMap<>();
        wrongs.stream()
                .filter(wrong -> "mastered".equals(wrong.getMasteryLevel()) && wrong.getLastReviewTime() != null)
                .sorted(Comparator.comparing(WrongQuestion::getLastReviewTime))
                .forEach(wrong -> masteryByDate.merge(wrong.getLastReviewTime().toLocalDate(), 1, Integer::sum));
        int running = 0;
        List<EfficiencyVO.MasteredGrowthPoint> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : masteryByDate.entrySet()) {
            running += entry.getValue();
            result.add(EfficiencyVO.MasteredGrowthPoint.builder()
                    .label(entry.getKey().toString())
                    .masteredCardCount(running)
                    .build());
        }
        return result;
    }

    private List<EfficiencyVO.CategoryMastery> buildCategoryMastery(Long userId, List<WrongQuestion> wrongs) {
        if (wrongs.isEmpty()) {
            return List.of();
        }

        Set<Long> questionIds = wrongs.stream().map(WrongQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty()
                ? Map.of()
                : questionMapper.selectBatchIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> categoryIds = questionMap.values().stream().map(Question::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryService.listByIds(categoryIds).forEach(category -> categoryNames.put(category.getId(), category.getName()));
        }

        Map<Long, List<WrongQuestion>> wrongsByCategory = new LinkedHashMap<>();
        for (WrongQuestion wrong : wrongs) {
            Question question = questionMap.get(wrong.getQuestionId());
            Long categoryId = question == null ? null : question.getCategoryId();
            wrongsByCategory.computeIfAbsent(categoryId == null ? -1L : categoryId, key -> new ArrayList<>()).add(wrong);
        }

        return wrongsByCategory.entrySet().stream()
                .map(entry -> {
                    List<WrongQuestion> items = entry.getValue();
                    int total = items.size();
                    int mastered = (int) items.stream().filter(item -> "mastered".equals(item.getMasteryLevel())).count();
                    int dueCount = (int) items.stream()
                            .filter(item -> item.getNextReviewDate() != null && !item.getNextReviewDate().isAfter(LocalDate.now()))
                            .count();
                    BigDecimal masteryRate = total <= 0
                            ? BigDecimal.ZERO
                            : BigDecimal.valueOf(mastered * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
                    return EfficiencyVO.CategoryMastery.builder()
                            .categoryId(entry.getKey() < 0 ? null : entry.getKey())
                            .categoryName(entry.getKey() < 0 ? "未分类" : categoryNames.getOrDefault(entry.getKey(), "未分类"))
                            .totalCards(total)
                            .masteredCards(mastered)
                            .dueCount(dueCount)
                            .masteryRate(masteryRate)
                            .build();
                })
                .sorted(Comparator.comparing(EfficiencyVO.CategoryMastery::getDueCount).reversed()
                        .thenComparing(EfficiencyVO.CategoryMastery::getMasteryRate))
                .toList();
    }

    private List<LearningInsightsVO.CategoryChange> buildCategoryChanges(List<InterviewSession> recentSessions, LocalDate thisMonday) {
        Set<Long> sessionIds = recentSessions.stream().map(InterviewSession::getId).collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return List.of();
        }

        List<InterviewRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<InterviewRecord>()
                        .in(InterviewRecord::getSessionId, sessionIds)
                        .isNotNull(InterviewRecord::getScore));
        Map<Long, InterviewSession> sessionMap = recentSessions.stream()
                .collect(Collectors.toMap(InterviewSession::getId, Function.identity(), (a, b) -> a));
        Set<Long> questionIds = records.stream().map(InterviewRecord::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of() : questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> categoryIds = questionMap.values().stream().map(Question::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNames = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryService.listByIds(categoryIds).forEach(category -> categoryNames.put(category.getId(), category.getName()));
        }

        Map<Long, Map<String, BigDecimal>> scoresByCategory = new HashMap<>();
        for (InterviewRecord record : records) {
            InterviewSession session = sessionMap.get(record.getSessionId());
            Question question = questionMap.get(record.getQuestionId());
            if (session == null || question == null || question.getCategoryId() == null || session.getStartTime() == null) {
                continue;
            }
            String period = session.getStartTime().toLocalDate().isBefore(thisMonday) ? "last" : "this";
            scoresByCategory.computeIfAbsent(question.getCategoryId(), key -> new HashMap<>()).merge(period, record.getScore(), BigDecimal::add);
            scoresByCategory.computeIfAbsent(question.getCategoryId(), key -> new HashMap<>()).merge(period + "_count", BigDecimal.ONE, BigDecimal::add);
        }

        return scoresByCategory.entrySet().stream()
                .map(entry -> {
                    BigDecimal thisScore = entry.getValue().getOrDefault("this", BigDecimal.ZERO);
                    BigDecimal thisCount = entry.getValue().getOrDefault("this_count", BigDecimal.ZERO);
                    BigDecimal lastScore = entry.getValue().getOrDefault("last", BigDecimal.ZERO);
                    BigDecimal lastCount = entry.getValue().getOrDefault("last_count", BigDecimal.ZERO);
                    BigDecimal thisWeekScore = thisCount.compareTo(BigDecimal.ZERO) > 0
                            ? thisScore.divide(thisCount, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal lastWeekScore = lastCount.compareTo(BigDecimal.ZERO) > 0
                            ? lastScore.divide(lastCount, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return LearningInsightsVO.CategoryChange.builder()
                            .categoryId(entry.getKey())
                            .categoryName(categoryNames.getOrDefault(entry.getKey(), "未分类"))
                            .thisWeekScore(thisWeekScore)
                            .lastWeekScore(lastWeekScore)
                            .change(thisWeekScore.subtract(lastWeekScore))
                            .build();
                })
                .sorted(Comparator.comparing((LearningInsightsVO.CategoryChange change) -> change.getChange().abs()).reversed())
                .toList();
    }

    private List<LearningInsightsVO.HourDistribution> analyzeBestHours(Long userId) {
        List<InterviewSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "finished")
                        .select(InterviewSession::getStartTime, InterviewSession::getTotalScore)
                        .isNotNull(InterviewSession::getStartTime)
                        .isNotNull(InterviewSession::getTotalScore)
                        .last("LIMIT 200"));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<String, List<BigDecimal>> buckets = new LinkedHashMap<>();
        buckets.put("上午 (6-12)", new ArrayList<>());
        buckets.put("下午 (12-18)", new ArrayList<>());
        buckets.put("晚上 (18-24)", new ArrayList<>());
        buckets.put("凌晨 (0-6)", new ArrayList<>());
        for (InterviewSession session : sessions) {
            int hour = session.getStartTime().getHour();
            String bucket = hour >= 6 && hour < 12 ? "上午 (6-12)"
                    : hour < 18 ? "下午 (12-18)"
                    : hour < 24 ? "晚上 (18-24)"
                    : "凌晨 (0-6)";
            buckets.get(bucket).add(session.getTotalScore());
        }

        return buckets.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> LearningInsightsVO.HourDistribution.builder()
                        .timeSlot(entry.getKey())
                        .sessionCount(entry.getValue().size())
                        .avgScore(avg(entry.getValue()))
                        .build())
                .sorted(Comparator.comparing(LearningInsightsVO.HourDistribution::getAvgScore).reversed())
                .toList();
    }

    private String resolveTodayCompletionStatus(List<ReviewLog> recentReviewLogs) {
        long todayCount = recentReviewLogs.stream()
                .filter(log -> log.getCreateTime() != null && log.getCreateTime().toLocalDate().equals(LocalDate.now()))
                .count();
        if (todayCount <= 0) {
            return "今天还没有开始错题复盘";
        }
        if (todayCount >= 5) {
            return "今天的复盘节奏已经拉起来了";
        }
        return String.format("今天已完成 %d 次错题复盘", todayCount);
    }

    private String resolveReviewDebtStatus(List<WrongQuestion> wrongs) {
        long dueCount = wrongs.stream()
                .filter(item -> item.getNextReviewDate() != null && !item.getNextReviewDate().isAfter(LocalDate.now()))
                .count();
        if (dueCount <= 0) {
            return "当前没有待处理的错题积压";
        }
        if (dueCount >= 10) {
            return String.format("高优先级积压 %d 题，建议先清理旧错题", dueCount);
        }
        return String.format("还有 %d 道错题待复盘", dueCount);
    }

    private String resolveMasteryGrowthStatus(List<WrongQuestion> wrongs) {
        long masteredCount = wrongs.stream().filter(item -> "mastered".equals(item.getMasteryLevel())).count();
        if (masteredCount <= 0) {
            return "还没有形成稳定掌握的题目";
        }
        if (masteredCount < 5) {
            return "掌握题量还在起步阶段";
        }
        return String.format("已稳定掌握 %d 道题，继续保持复盘节奏", masteredCount);
    }

    private String resolvePlanExecutionStatus(StudyPlan activePlan, int todayCompletedTaskCount, int todayPlanTaskCount) {
        if (activePlan == null) {
            return "还没有训练计划";
        }
        if (todayPlanTaskCount <= 0) {
            return "今天没有待执行的计划任务";
        }
        if (todayCompletedTaskCount >= todayPlanTaskCount) {
            return "今天的计划任务已经完成";
        }
        return String.format("今天已完成 %d / %d 项计划任务", todayCompletedTaskCount, todayPlanTaskCount);
    }

    private String resolveApplicationStatus(List<JobApplication> applications, int activeApplicationCount, int offerCount) {
        if (applications.isEmpty()) {
            return "还没有投递记录";
        }
        if (offerCount > 0) {
            return String.format("已有 %d 个 Offer，继续稳住后续流程", offerCount);
        }
        if (activeApplicationCount > 0) {
            return String.format("当前有 %d 条投递在推进中", activeApplicationCount);
        }
        return String.format("已记录 %d 条投递，下一步可以补更多岗位", applications.size());
    }

    private String resolveResumeReadinessStatus(List<ResumeFile> resumes, ResumeFile latestResume) {
        if (resumes.isEmpty()) {
            return "还没有上传简历";
        }
        if (latestResume != null && "parsed".equals(latestResume.getParseStatus())) {
            return String.format("最新简历《%s》已经可以继续整理项目表达", latestResume.getTitle());
        }
        return String.format("最新简历《%s》还在处理中", latestResume == null ? "未命名简历" : latestResume.getTitle());
    }

    private String resolveInterviewConversionStatus(int thisWeekInterviewCount, int activeApplicationCount, int offerCount) {
        if (offerCount > 0) {
            return "已有面试结果进入 Offer 阶段";
        }
        if (activeApplicationCount > 0 && thisWeekInterviewCount == 0) {
            return "有投递在推进中，建议尽快补一轮模拟面试";
        }
        if (thisWeekInterviewCount > 0) {
            return String.format("本周已完成 %d 场模拟面试，可以继续对照真实岗位推进", thisWeekInterviewCount);
        }
        return "先做一轮模拟面试，建立当前表达基线";
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatWeek(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private BigDecimal avg(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private String resolveWrongContentType(Long userId, Long wrongQuestionId) {
        WrongQuestion wrongQuestion = wrongQuestionMapper.selectById(wrongQuestionId);
        if (wrongQuestion == null || !userId.equals(wrongQuestion.getUserId())) {
            return "wrong_card";
        }
        return "interview".equalsIgnoreCase(wrongQuestion.getSourceType()) ? "interview_card" : "wrong_card";
    }

    private record MergedLogPoint(
            String week,
            BigDecimal easeFactorAfter,
            Integer rating,
            String contentType,
            java.time.LocalDateTime createTime
    ) {
    }

    private record TrendData(
            List<String> weeks,
            List<TrendVO.WeeklyPoint> overallTrend,
            List<TrendVO.CategoryTrend> categoryTrends
    ) {
    }
}
