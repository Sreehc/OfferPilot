package com.offerpilot.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationEventMapper;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.RecentInterviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.mapper.DashboardMetricsMapper;
import com.offerpilot.dashboard.service.impl.DashboardServiceImpl;
import com.offerpilot.interview.mapper.CopilotPrepSessionMapper;
import com.offerpilot.interview.mapper.CopilotRealtimeSessionMapper;
import com.offerpilot.interview.mapper.JobPrepSessionMapper;
import com.offerpilot.interview.mapper.RecordingReviewSessionMapper;
import com.offerpilot.plan.entity.StudyPlan;
import com.offerpilot.plan.entity.StudyPlanTask;
import com.offerpilot.plan.mapper.StudyPlanMapper;
import com.offerpilot.plan.mapper.StudyPlanTaskMapper;
import com.offerpilot.resume.entity.ResumeFile;
import com.offerpilot.resume.mapper.ResumeFileMapper;
import com.offerpilot.security.util.SecurityUtils;
import com.offerpilot.wrong.entity.ReviewLog;
import com.offerpilot.wrong.mapper.ReviewLogMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        TableInfoHelper.initTableInfo(
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "test"),
                ReviewLog.class);
    }

    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AdaptiveService adaptiveService;
    @Mock
    private OfferPilotProperties props;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private JobApplicationEventMapper jobApplicationEventMapper;
    @Mock
    private ReviewLogMapper reviewLogMapper;
    @Mock
    private StudyPlanMapper studyPlanMapper;
    @Mock
    private StudyPlanTaskMapper studyPlanTaskMapper;
    @Mock
    private ResumeFileMapper resumeFileMapper;
    @Mock
    private AgentRunMapper agentRunMapper;
    @Mock
    private JobPrepSessionMapper jobPrepSessionMapper;
    @Mock
    private CopilotPrepSessionMapper copilotPrepSessionMapper;
    @Mock
    private CopilotRealtimeSessionMapper copilotRealtimeSessionMapper;
    @Mock
    private RecordingReviewSessionMapper recordingReviewSessionMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(jobApplicationEventMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void overview_emptyData_marksFirstVisitAndBuildsEmptyApplicationSummary() throws Exception {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);

            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(null);
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(null);
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(null);
            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(studyPlanMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(null);
            when(resumeFileMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder().build());
            doReturn("{}").when(objectMapper)
                    .writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class));

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(0, result.getLearningCount());
            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getWrongCount());
            assertTrue(result.getFirstVisit());
            assertTrue(result.getRecentInterviews().isEmpty());
            assertTrue(result.getWeakPoints().isEmpty());
            assertEquals(0, result.getReviewDebtCount());
            assertEquals(0, result.getStudyStreak());
            assertEquals("upload_resume", result.getNextAction().getKey());
            assertEquals("/resume#resume-upload", result.getNextAction().getPath());
            assertEquals(0, result.getApplicationSummary().getTotalCount());
            assertEquals("还没有投递记录", result.getApplicationSummary().getTopCompany());
            assertTrue(result.getWorkflowContinuations().isEmpty());
        }
    }

    @Test
    void overview_hasData_buildsApplicationSummaryAndNotFirstVisit() throws Exception {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);

            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(5L);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(3L);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(new BigDecimal("78.50"));
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(10L);
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(4L);

            RecentInterviewVO recent = new RecentInterviewVO();
            recent.setSessionId(1L);
            recent.setDirection("Java Backend");
            recent.setTotalScore(new BigDecimal("80"));
            recent.setStatus("finished");
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(List.of(recent));

            WeakPointVO weak = new WeakPointVO();
            weak.setCategoryName("JVM");
            weak.setWrongCount(5);
            weak.setScore(new BigDecimal("60"));
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(List.of(weak));

            JobApplication active = new JobApplication();
            active.setStatus("interview");
            active.setCompany("Alpha");
            active.setMatchScore(new BigDecimal("85"));

            JobApplication offer = new JobApplication();
            offer.setStatus("offer");
            offer.setCompany("Beta");
            offer.setMatchScore(new BigDecimal("95"));

            ResumeFile resume = new ResumeFile();
            resume.setTitle("Java后端简历");

            StudyPlan activePlan = new StudyPlan();
            activePlan.setId(10L);
            activePlan.setCurrentDay(2);
            activePlan.setStatus("active");

            StudyPlanTask pendingTask = new StudyPlanTask();
            pendingTask.setId(100L);
            pendingTask.setDayIndex(2);
            pendingTask.setTaskDate(LocalDate.now());
            pendingTask.setStatus("pending");

            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(active, offer));
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(studyPlanMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(activePlan);
            when(studyPlanTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(pendingTask));
            when(resumeFileMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(resume));
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(
                    AbilityProfileVO.builder()
                            .weakCategories(List.of("JVM"))
                            .suggestedFocus("JVM")
                            .build());
            doReturn("{}").when(objectMapper)
                    .writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class));

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(8, result.getLearningCount());
            assertEquals(new BigDecimal("78.50"), result.getAverageScore());
            assertEquals(10, result.getWrongCount());
            assertFalse(result.getFirstVisit());
            assertEquals(1, result.getRecentInterviews().size());
            assertEquals(1, result.getWeakPoints().size());
            assertEquals(4, result.getReviewDebtCount());
            assertEquals("complete_today_plan", result.getNextAction().getKey());
            assertEquals("/study-plan", result.getNextAction().getPath());
            assertEquals(2, result.getApplicationSummary().getTotalCount());
            assertEquals(1, result.getApplicationSummary().getActiveCount());
            assertEquals(1, result.getApplicationSummary().getOfferCount());
            assertEquals(new BigDecimal("90.00"), result.getApplicationSummary().getAverageMatchScore());
            assertEquals("Beta", result.getApplicationSummary().getTopCompany());
            verify(valueOperations).set(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void overview_returnsCachedPayloadWhenPresent() throws Exception {
        DashboardOverviewVO cached = DashboardOverviewVO.builder()
                .learningCount(2)
                .averageScore(new BigDecimal("88"))
                .wrongCount(1)
                .recentInterviews(List.of())
                .weakPoints(List.of())
                .firstVisit(false)
                .build();

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn("{cached}");
            when(objectMapper.readValue("{cached}", DashboardOverviewVO.class)).thenReturn(cached);

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals(2, result.getLearningCount());
            verify(dashboardMetricsMapper, never()).countChatSessions(1L);
        }
    }

    @Test
    void overview_prefersJobPrepWhenTodayPlanIsDoneAndApplicationActive() throws Exception {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);
            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(1L);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(1L);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(BigDecimal.TEN);
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(1L);
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(List.of());
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(List.of());
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(0L);
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

            ResumeFile resume = new ResumeFile();
            resume.setTitle("已上传简历");
            when(resumeFileMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(resume));

            StudyPlan activePlan = new StudyPlan();
            activePlan.setId(10L);
            activePlan.setCurrentDay(1);
            activePlan.setStatus("active");
            when(studyPlanMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(activePlan);

            StudyPlanTask doneTask = new StudyPlanTask();
            doneTask.setStatus("completed");
            doneTask.setDayIndex(1);
            when(studyPlanTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(doneTask));

            JobApplication active = new JobApplication();
            active.setStatus("applied");
            active.setCompany("Gamma");
            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(active));
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder().build());
            doReturn("{}").when(objectMapper)
                    .writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class));

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals("job_prep", result.getNextAction().getKey());
            assertEquals("/interview?workspace=job-prep", result.getNextAction().getPath());
        }
    }

    @Test
    void overview_recommendsMockInterviewWorkspaceWhenCoreInputsAreReady() throws Exception {
        OfferPilotProperties.Dashboard dashboardProps = new OfferPilotProperties.Dashboard();
        dashboardProps.setCacheTtlMinutes(5);

        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("dashboard:overview:1")).thenReturn(null);
            when(props.getDashboard()).thenReturn(dashboardProps);
            when(dashboardMetricsMapper.countChatSessions(1L)).thenReturn(3L);
            when(dashboardMetricsMapper.countInterviewSessions(1L)).thenReturn(2L);
            when(dashboardMetricsMapper.averageInterviewScore(1L)).thenReturn(new BigDecimal("82"));
            when(dashboardMetricsMapper.countWrongQuestions(1L)).thenReturn(1L);
            when(dashboardMetricsMapper.selectRecentInterviews(1L)).thenReturn(List.of());
            when(dashboardMetricsMapper.selectWeakPoints(1L)).thenReturn(List.of());
            when(dashboardMetricsMapper.countReviewDebt(1L)).thenReturn(0L);
            when(reviewLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

            ResumeFile resume = new ResumeFile();
            resume.setTitle("已上传简历");
            when(resumeFileMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(resume));

            StudyPlan activePlan = new StudyPlan();
            activePlan.setId(10L);
            activePlan.setCurrentDay(2);
            activePlan.setStatus("active");
            when(studyPlanMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(activePlan);

            StudyPlanTask doneTask = new StudyPlanTask();
            doneTask.setStatus("completed");
            doneTask.setDayIndex(2);
            when(studyPlanTaskMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(doneTask));

            when(jobApplicationMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
            when(adaptiveService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder().build());
            doReturn("{}").when(objectMapper)
                    .writeValueAsString(org.mockito.ArgumentMatchers.any(DashboardOverviewVO.class));

            DashboardOverviewVO result = dashboardService.overview();

            assertEquals("start_interview", result.getNextAction().getKey());
            assertEquals("/interview?workspace=mock-interview", result.getNextAction().getPath());
        }
    }

    @Test
    void overview_noUserId_throws() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(null);

            assertThrows(Exception.class, () -> dashboardService.overview());
        }
    }
}
