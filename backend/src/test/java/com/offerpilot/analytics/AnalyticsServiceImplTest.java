package com.offerpilot.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.offerpilot.analytics.service.impl.AnalyticsServiceImpl;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
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
}
