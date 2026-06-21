package com.offerpilot.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.analytics.service.impl.AnalyticsServiceImpl;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.ProfileTopicRetrospectiveVO;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.category.entity.Category;
import com.offerpilot.category.service.CategoryService;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisMapperBuilderAssistant assistant =
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, InterviewSession.class);
        TableInfoHelper.initTableInfo(assistant, InterviewRecord.class);
        TableInfoHelper.initTableInfo(assistant, WrongQuestion.class);
        TableInfoHelper.initTableInfo(assistant, ReviewLog.class);
        TableInfoHelper.initTableInfo(assistant, StudyPlan.class);
        TableInfoHelper.initTableInfo(assistant, StudyPlanTask.class);
        TableInfoHelper.initTableInfo(assistant, ResumeFile.class);
        TableInfoHelper.initTableInfo(assistant, JobApplication.class);
        TableInfoHelper.initTableInfo(assistant, Question.class);
        TableInfoHelper.initTableInfo(assistant, Category.class);
    }

    @Mock
    private InterviewRecordMapper recordMapper;
    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private CategoryService categoryService;
    @Mock
    private WrongQuestionMapper wrongQuestionMapper;
    @Mock
    private ReviewLogMapper reviewLogMapper;
    @Mock
    private StudyPlanMapper studyPlanMapper;
    @Mock
    private StudyPlanTaskMapper studyPlanTaskMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private AdaptiveService adaptiveService;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getEfficiencyData_returnsRealReviewSignalsWithoutSyntheticCompletionRate() {
        WrongQuestion interviewWrong = new WrongQuestion();
        interviewWrong.setId(11L);
        interviewWrong.setUserId(1L);
        interviewWrong.setQuestionId(101L);
        interviewWrong.setSourceType("interview");
        interviewWrong.setMasteryLevel("reviewing");
        interviewWrong.setEaseFactor(new BigDecimal("2.30"));
        interviewWrong.setNextReviewDate(LocalDate.now().minusDays(1));
        interviewWrong.setLastReviewTime(LocalDateTime.now().minusDays(1));

        WrongQuestion masteredWrong = new WrongQuestion();
        masteredWrong.setId(12L);
        masteredWrong.setUserId(1L);
        masteredWrong.setQuestionId(102L);
        masteredWrong.setSourceType("practice");
        masteredWrong.setMasteryLevel("mastered");
        masteredWrong.setEaseFactor(new BigDecimal("2.80"));
        masteredWrong.setNextReviewDate(LocalDate.now().plusDays(2));
        masteredWrong.setLastReviewTime(LocalDateTime.now());

        ReviewLog againLog = new ReviewLog();
        againLog.setUserId(1L);
        againLog.setWrongQuestionId(11L);
        againLog.setRating(1);
        againLog.setEaseFactorAfter(new BigDecimal("2.30"));
        againLog.setCreateTime(LocalDateTime.now().minusDays(1));

        ReviewLog goodLog = new ReviewLog();
        goodLog.setUserId(1L);
        goodLog.setWrongQuestionId(12L);
        goodLog.setRating(4);
        goodLog.setEaseFactorAfter(new BigDecimal("2.80"));
        goodLog.setCreateTime(LocalDateTime.now());

        Question redis = new Question();
        redis.setId(101L);
        redis.setCategoryId(7L);

        Question jvm = new Question();
        jvm.setId(102L);
        jvm.setCategoryId(8L);

        Category redisCategory = new Category();
        redisCategory.setId(7L);
        redisCategory.setName("Redis");

        Category jvmCategory = new Category();
        jvmCategory.setId(8L);
        jvmCategory.setName("JVM");

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(interviewWrong, masteredWrong));
        when(reviewLogMapper.selectList(any())).thenReturn(List.of(againLog, goodLog));
        when(wrongQuestionMapper.selectById(11L)).thenReturn(interviewWrong);
        when(wrongQuestionMapper.selectById(12L)).thenReturn(masteredWrong);
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(redis, jvm));
        when(categoryService.listByIds(any())).thenReturn(List.of(redisCategory, jvmCategory));

        EfficiencyVO result = analyticsService.getEfficiencyData(1L);

        assertEquals(new BigDecimal("2.55"), result.getAvgEaseFactor());
        assertEquals(2, result.getTotalReviews());
        assertEquals(new BigDecimal("50.00"), result.getForgettingRate());
        assertEquals(1L, result.getRatingDistribution().get(1));
        assertEquals(1L, result.getRatingDistribution().get(4));
        assertEquals(1L, result.getContentTypeDistribution().get("interview_card"));
        assertEquals(1L, result.getContentTypeDistribution().get("wrong_card"));
        assertFalse(result.getCategoryMastery().isEmpty());
        assertFalse(result.getReviewDebtTrend().isEmpty());
    }

    @Test
    void getLearningInsights_reusesDashboardNextActionAndSummarizesCurrentState() {
        NextActionVO nextAction = NextActionVO.builder()
                .key("follow_application")
                .title("继续跟进投递")
                .description("今天先跟进推进中的岗位")
                .path("/applications")
                .reason("application_in_progress")
                .priority("P1")
                .build();

        DashboardOverviewVO overview = DashboardOverviewVO.builder()
                .nextAction(nextAction)
                .build();

        InterviewSession thisWeekSession = new InterviewSession();
        thisWeekSession.setId(1L);
        thisWeekSession.setUserId(1L);
        thisWeekSession.setStatus("finished");
        thisWeekSession.setTotalScore(new BigDecimal("82"));
        thisWeekSession.setStartTime(LocalDate.now().atTime(10, 0));
        thisWeekSession.setCreateTime(LocalDateTime.now());

        InterviewSession lastWeekSession = new InterviewSession();
        lastWeekSession.setId(2L);
        lastWeekSession.setUserId(1L);
        lastWeekSession.setStatus("finished");
        lastWeekSession.setTotalScore(new BigDecimal("74"));
        lastWeekSession.setStartTime(LocalDate.now().minusWeeks(1).atTime(20, 0));
        lastWeekSession.setCreateTime(LocalDateTime.now().minusWeeks(1));

        InterviewRecord thisWeekRecord = new InterviewRecord();
        thisWeekRecord.setSessionId(1L);
        thisWeekRecord.setQuestionId(101L);
        thisWeekRecord.setScore(new BigDecimal("82"));

        InterviewRecord lastWeekRecord = new InterviewRecord();
        lastWeekRecord.setSessionId(2L);
        lastWeekRecord.setQuestionId(102L);
        lastWeekRecord.setScore(new BigDecimal("74"));

        Question redis = new Question();
        redis.setId(101L);
        redis.setCategoryId(7L);

        Question jvm = new Question();
        jvm.setId(102L);
        jvm.setCategoryId(8L);

        Category redisCategory = new Category();
        redisCategory.setId(7L);
        redisCategory.setName("Redis");

        Category jvmCategory = new Category();
        jvmCategory.setId(8L);
        jvmCategory.setName("JVM");

        WrongQuestion dueWrong = new WrongQuestion();
        dueWrong.setUserId(1L);
        dueWrong.setMasteryLevel("reviewing");
        dueWrong.setNextReviewDate(LocalDate.now());

        ReviewLog todayReview = new ReviewLog();
        todayReview.setUserId(1L);
        todayReview.setCreateTime(LocalDateTime.now());

        StudyPlan activePlan = new StudyPlan();
        activePlan.setId(20L);
        activePlan.setTitle("本周强化计划");
        activePlan.setProgressRate(new BigDecimal("50"));

        StudyPlanTask doneTask = new StudyPlanTask();
        doneTask.setStatus("completed");

        StudyPlanTask pendingTask = new StudyPlanTask();
        pendingTask.setStatus("pending");

        JobApplication activeApplication = new JobApplication();
        activeApplication.setUserId(1L);
        activeApplication.setStatus("interview");

        ResumeFile resume = new ResumeFile();
        resume.setTitle("Java 后端简历");
        resume.setParseStatus("parsed");

        when(sessionMapper.selectList(any())).thenReturn(List.of(lastWeekSession, thisWeekSession), List.of(thisWeekSession, lastWeekSession));
        when(recordMapper.selectList(any())).thenReturn(List.of(thisWeekRecord, lastWeekRecord));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(redis, jvm));
        when(categoryService.listByIds(any())).thenReturn(List.of(redisCategory, jvmCategory));
        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(dueWrong));
        when(reviewLogMapper.selectList(any())).thenReturn(List.of(todayReview));
        when(studyPlanMapper.selectOne(any())).thenReturn(activePlan);
        when(studyPlanTaskMapper.selectList(any())).thenReturn(List.of(doneTask, pendingTask));
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of(activeApplication));
        when(resumeFileMapper.selectList(any())).thenReturn(List.of(resume));
        when(dashboardService.overview()).thenReturn(overview);

        LearningInsightsVO result = analyticsService.getLearningInsights(1L);

        assertNotNull(result.getNextAction());
        assertEquals("follow_application", result.getNextAction().getKey());
        assertEquals("/applications", result.getNextAction().getPath());
        assertEquals("今天已完成 1 / 2 项计划任务", result.getPlanExecutionStatus());
        assertEquals("当前有 1 条投递在推进中", result.getApplicationStatus());
        assertEquals("最新简历《Java 后端简历》已经可以继续整理项目表达", result.getResumeReadinessStatus());
        assertEquals("本周已完成 1 场模拟面试，可以继续对照真实岗位推进", result.getInterviewConversionStatus());
        assertFalse(result.getCategoryChanges().isEmpty());
        assertFalse(result.getBestStudyHours().isEmpty());
        verify(dashboardService).overview();
    }

    @Test
    void getProfileTopicDetail_includesRecordingReviewEvidenceInSummaryAndRecommendations() {
        when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .overallAbility(68.0)
                .recommendedDifficulty("medium")
                .recordingReviewCount(1)
                .copilotRealtimeCount(1)
                .categoryAbilities(List.of(CategoryAbilityVO.builder()
                        .categoryId(7L)
                        .categoryName("Redis")
                        .abilityScore(63.0)
                        .interviewCount(2)
                        .recordingReviewCount(1)
                        .jobPrepCount(1)
                        .copilotPrepCount(1)
                        .copilotRealtimeCount(1)
                        .applicationFeedbackCount(1)
                        .resumeEvidenceCount(1)
                        .wrongCount(1)
                        .isWeak(false)
                        .recommendedDifficulty("medium")
                        .build()))
                .weakCategories(List.of())
                .suggestedFocus("Redis")
                .build());

        WrongQuestion dueWrong = new WrongQuestion();
        dueWrong.setId(11L);
        dueWrong.setUserId(1L);
        dueWrong.setQuestionId(101L);
        dueWrong.setMasteryLevel("reviewing");
        dueWrong.setNextReviewDate(LocalDate.now());

        InterviewRecord recentRecord = new InterviewRecord();
        recentRecord.setSessionId(1L);
        recentRecord.setQuestionId(101L);
        recentRecord.setScore(new BigDecimal("72"));
        recentRecord.setUserId(1L);
        recentRecord.setCreateTime(LocalDateTime.now());

        InterviewRecord olderRecord = new InterviewRecord();
        olderRecord.setSessionId(2L);
        olderRecord.setQuestionId(101L);
        olderRecord.setScore(new BigDecimal("60"));
        olderRecord.setUserId(1L);
        olderRecord.setCreateTime(LocalDateTime.now().minusWeeks(1));

        InterviewSession recentSession = new InterviewSession();
        recentSession.setId(1L);
        recentSession.setStartTime(LocalDate.now().atTime(10, 0));

        InterviewSession olderSession = new InterviewSession();
        olderSession.setId(2L);
        olderSession.setStartTime(LocalDate.now().minusWeeks(1).atTime(10, 0));

        Question redis = new Question();
        redis.setId(101L);
        redis.setCategoryId(7L);

        Category redisCategory = new Category();
        redisCategory.setId(7L);
        redisCategory.setName("Redis");

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(dueWrong));
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());
        when(recordMapper.selectList(any())).thenReturn(List.of(recentRecord, olderRecord));
        when(sessionMapper.selectBatchIds(any())).thenReturn(List.of(recentSession, olderSession));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(redis));
        when(categoryService.listByIds(any())).thenReturn(List.of(redisCategory));

        ProfileTopicDetailVO detail = analyticsService.getProfileTopicDetail(1L, 7L);

        assertEquals(1, detail.getRecordingReviewCount());
        assertEquals(1, detail.getJobPrepCount());
        assertEquals(1, detail.getCopilotPrepCount());
        assertEquals(1, detail.getCopilotRealtimeCount());
        assertEquals(1, detail.getApplicationFeedbackCount());
        assertEquals(1, detail.getResumeEvidenceCount());
        assertEquals("ready", detail.getEvidenceStatus());
        assertEquals(Boolean.TRUE, detail.getRetrospectiveReady());
        assertTrue(detail.getSummary().contains("真实录音复盘证据"));
        assertTrue(detail.getSummary().contains("Copilot Prep"));
        assertTrue(detail.getSummary().contains("实时 Copilot"));
        assertTrue(detail.getSummary().contains("投递反馈证据"));
        assertTrue(detail.getSummary().contains("简历表达证据"));
        assertTrue(detail.getFocusRecommendations().stream()
                .anyMatch(item -> item.contains("真实录音复盘里暴露的表达问题")));
        assertTrue(detail.getFocusRecommendations().stream()
                .anyMatch(item -> item.contains("岗位化表达")));
        assertTrue(detail.getFocusRecommendations().stream()
                .anyMatch(item -> item.contains("JD 备面") || item.contains("Copilot Prep")));
        assertTrue(detail.getFocusRecommendations().stream()
                .anyMatch(item -> item.contains("投递反馈缺口")));
        assertTrue(detail.getFocusRecommendations().stream()
                .anyMatch(item -> item.contains("项目表达") || item.contains("案例")));
    }

    @Test
    void buildProfileTopicRetrospective_withoutRecordingReviewEvidence_surfacesRiskAndAction() {
        when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .overallAbility(52.0)
                .recommendedDifficulty("easy")
                .recordingReviewCount(0)
                .copilotRealtimeCount(0)
                .categoryAbilities(List.of(CategoryAbilityVO.builder()
                        .categoryId(8L)
                        .categoryName("JVM")
                        .abilityScore(58.0)
                        .interviewCount(1)
                        .recordingReviewCount(0)
                        .jobPrepCount(0)
                        .copilotPrepCount(0)
                        .copilotRealtimeCount(0)
                        .applicationFeedbackCount(0)
                        .resumeEvidenceCount(0)
                        .wrongCount(2)
                        .isWeak(true)
                        .recommendedDifficulty("easy")
                        .build()))
                .weakCategories(List.of("JVM"))
                .suggestedFocus("JVM")
                .build());

        WrongQuestion dueWrong = new WrongQuestion();
        dueWrong.setId(12L);
        dueWrong.setUserId(1L);
        dueWrong.setQuestionId(102L);
        dueWrong.setMasteryLevel("reviewing");
        dueWrong.setNextReviewDate(LocalDate.now().minusDays(1));

        InterviewRecord record = new InterviewRecord();
        record.setSessionId(3L);
        record.setQuestionId(102L);
        record.setScore(new BigDecimal("58"));
        record.setUserId(1L);
        record.setCreateTime(LocalDateTime.now());

        InterviewSession session = new InterviewSession();
        session.setId(3L);
        session.setUserId(1L);
        session.setStatus("finished");
        session.setTotalScore(new BigDecimal("58"));
        session.setStartTime(LocalDate.now().atTime(20, 0));
        session.setCreateTime(LocalDateTime.now());

        Question jvm = new Question();
        jvm.setId(102L);
        jvm.setCategoryId(8L);

        Category jvmCategory = new Category();
        jvmCategory.setId(8L);
        jvmCategory.setName("JVM");

        when(wrongQuestionMapper.selectList(any())).thenReturn(List.of(dueWrong), List.of(dueWrong));
        when(reviewLogMapper.selectList(any())).thenReturn(List.of());
        when(recordMapper.selectList(any())).thenReturn(List.of(record), List.of(record));
        when(sessionMapper.selectBatchIds(any())).thenReturn(List.of(session));
        when(sessionMapper.selectList(any())).thenReturn(List.of(session), List.of(session));
        when(questionMapper.selectBatchIds(any())).thenReturn(List.of(jvm));
        when(categoryService.listByIds(any())).thenReturn(List.of(jvmCategory));
        when(studyPlanMapper.selectOne(any())).thenReturn(null);
        when(jobApplicationMapper.selectList(any())).thenReturn(List.of());
        when(resumeFileMapper.selectList(any())).thenReturn(List.of());
        when(dashboardService.overview()).thenReturn(DashboardOverviewVO.builder().build());

        ProfileTopicRetrospectiveVO retrospective = analyticsService.buildProfileTopicRetrospective(1L, 8L);

        assertEquals("forming", retrospective.getEvidenceStatus());
        assertTrue(retrospective.getEvidenceSummary().contains("画像还在形成中"));
        assertTrue(retrospective.getRiskSignals().stream()
                .anyMatch(item -> item.contains("缺真实录音复盘证据")));
        assertTrue(retrospective.getNextActions().stream()
                .anyMatch(item -> item.contains("补 1 次真实录音复盘")));
        assertTrue(retrospective.getRiskSignals().stream()
                .anyMatch(item -> item.contains("缺岗位化 Prep 证据")));
        assertTrue(retrospective.getNextActions().stream()
                .anyMatch(item -> item.contains("JD 定向备面") || item.contains("Copilot Prep")));
        assertTrue(retrospective.getRiskSignals().stream()
                .anyMatch(item -> item.contains("缺真实投递反馈证据")));
        assertTrue(retrospective.getNextActions().stream()
                .anyMatch(item -> item.contains("真实投递推进反馈")));
        assertTrue(retrospective.getRiskSignals().stream()
                .anyMatch(item -> item.contains("缺简历项目表达证据")));
        assertTrue(retrospective.getNextActions().stream()
                .anyMatch(item -> item.contains("简历项目表达") || item.contains("自我介绍")));
    }
}
