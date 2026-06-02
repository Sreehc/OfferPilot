package com.offerpilot.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.entity.AgentRun;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.agent.service.impl.AgentRunServiceImpl;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.AgentRunVO;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.ProfileTopicRetrospectiveVO;
import com.offerpilot.application.service.JobApplicationService;
import com.offerpilot.application.vo.JobApplicationVO;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.NextActionVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import com.offerpilot.plan.service.PlanService;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;
import com.offerpilot.resume.service.ResumeService;
import com.offerpilot.resume.vo.ResumeFileVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentRunServiceImplTest {

    private AgentRun lastInsertedRun;

    @Mock
    private AgentRunMapper agentRunMapper;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private PlanService planService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private InterviewService interviewService;
    @Mock
    private InterviewRecordingReviewService interviewRecordingReviewService;
    @Mock
    private InterviewJobPrepService interviewJobPrepService;
    @Mock
    private InterviewCopilotRealtimeService interviewCopilotRealtimeService;
    @Mock
    private ResumeService resumeService;
    @Mock
    private JobApplicationService jobApplicationService;
    @Mock
    private UserProviderConfigService userProviderConfigService;

    @InjectMocks
    private AgentRunServiceImpl agentRunService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            run.setId(1001L);
            run.setUpdateTime(LocalDateTime.of(2026, 6, 2, 10, 0));
            lastInsertedRun = run;
            return 1;
        }).when(agentRunMapper).insert(any(AgentRun.class));
        lenient().when(agentRunMapper.selectById(1001L)).thenAnswer(invocation -> lastInsertedRun);
        lenient().doAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            lastInsertedRun = run;
            return 1;
        }).when(agentRunMapper).updateById(any(AgentRun.class));
    }

    @Test
    void createRun_studyPlannerBuildsContextAwarePayload() throws Exception {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .overallAbility(63D)
                .recommendedDifficulty("medium")
                .suggestedFocus("JVM")
                .weakCategories(List.of("JVM"))
                .build());
        when(analyticsService.getProfileTopicDetail(1L, 12L)).thenReturn(ProfileTopicDetailVO.builder()
                .categoryId(12L)
                .categoryName("JVM")
                .abilityScore(61D)
                .dueCount(3)
                .focusRecommendations(List.of("先补 JVM 内存区域。", "补一轮 GC 追问。"))
                .build());
        when(interviewService.detail(1L, 88L)).thenReturn(InterviewDetailVO.builder()
                .sessionId(88L)
                .direction("Java 后端")
                .jobRole("Java后端开发")
                .techStack("Spring Boot, Redis")
                .records(List.of(
                        InterviewDetailVO.InterviewRecordVO.builder()
                                .questionId(1L)
                                .questionTitle("JVM 调优")
                                .isLowScore(true)
                                .reviewSummary("回答停留在概念，缺少排查思路。")
                                .weakPointTags(List.of("JVM", "排查"))
                                .build()))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("analytics:profile", "analytics:topic:12", "interview:session:88"),
                "准备下周一面"));

        assertTrue(result.getSummary().contains("JVM"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("画像分 61")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("准备下周一面")));
        assertEquals("/analytics", result.getNextActionPath());
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getApprovalPayloadJson());
        assertEquals("JVM", payload.get("focusDirection").asText());
        assertEquals("Java后端开发", payload.get("targetRole").asText());
        assertEquals("Spring Boot, Redis", payload.get("techStack").asText());
    }

    @Test
    void createRun_studyPlannerUsesTopicRetrospectiveContext() {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .suggestedFocus("JVM")
                .recommendedDifficulty("medium")
                .build());
        when(analyticsService.getProfileTopicDetail(1L, 12L)).thenReturn(ProfileTopicDetailVO.builder()
                .categoryId(12L)
                .categoryName("JVM")
                .abilityScore(58D)
                .dueCount(4)
                .focusRecommendations(List.of("先补 JVM 内存模型。"))
                .build());
        when(analyticsService.buildProfileTopicRetrospective(1L, 12L)).thenReturn(ProfileTopicRetrospectiveVO.builder()
                .categoryId(12L)
                .categoryName("JVM")
                .stage("needs_attention")
                .summary("当前仍是需要优先处理的薄弱领域。")
                .riskSignals(List.of("画像分仍处在较低区间。", "还有 4 个待复盘点没有清完。"))
                .nextActions(List.of("先围绕 JVM 安排 1 轮专项题库训练。", "清理待复盘点后再补一场定向模拟。"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("analytics:profile", "analytics:topic:12", "analytics:retrospective:topic:12", "study-plan:active"),
                "把领域回顾转成下周训练重点"));

        assertTrue(result.getSummary().contains("JVM"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("领域回顾提示当前风险")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("专项题库训练")));
        assertEquals("/analytics", result.getNextActionPath());
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));
        assertEquals("save_topic_retrospective_action", result.getApprovalActionType());
    }

    @Test
    void createRun_studyPlannerUsesActivePlanContext() throws Exception {
        when(planService.current(1L)).thenReturn(StudyPlanCurrentVO.builder()
                .id(91L)
                .title("JVM 一周强化")
                .durationDays(7)
                .currentDay(3)
                .todayTaskCount(2)
                .focusDirection("JVM")
                .targetRole("Java后端开发")
                .techStack("Spring Boot, Redis")
                .todayFocusSummary(StudyPlanCurrentVO.TodayFocusSummaryVO.builder()
                        .reason("先清 JVM 内存模型和 GC 的待复盘点。")
                        .build())
                .tasks(List.of(
                        StudyPlanCurrentVO.StudyPlanTaskVO.builder()
                                .id(501L)
                                .dayIndex(3)
                                .module("review")
                                .title("JVM 待复盘清理")
                                .status("pending")
                                .build()))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("study-plan:active"),
                "把今天的任务和下轮刷新顺起来"));

        assertTrue(result.getSummary().contains("画像"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("当前正式计划是《JVM 一周强化》")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("今天先推进")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("JVM 待复盘清理")));

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getApprovalPayloadJson());
        assertEquals("JVM", payload.get("focusDirection").asText());
        assertEquals("Java后端开发", payload.get("targetRole").asText());
        assertEquals("Spring Boot, Redis", payload.get("techStack").asText());
    }

    @Test
    void approveRun_persistsTopicRetrospectiveAsFormalTrainingAction() {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .suggestedFocus("JVM")
                .recommendedDifficulty("medium")
                .build());
        when(analyticsService.getProfileTopicDetail(1L, 12L)).thenReturn(ProfileTopicDetailVO.builder()
                .categoryId(12L)
                .categoryName("JVM")
                .abilityScore(58D)
                .dueCount(4)
                .focusRecommendations(List.of("先补 JVM 内存模型。"))
                .build());
        when(analyticsService.buildProfileTopicRetrospective(1L, 12L)).thenReturn(ProfileTopicRetrospectiveVO.builder()
                .categoryId(12L)
                .categoryName("JVM")
                .stage("needs_attention")
                .summary("当前仍是需要优先处理的薄弱领域。")
                .riskSignals(List.of("画像分仍处在较低区间。", "还有 4 个待复盘点没有清完。"))
                .nextActions(List.of("先围绕 JVM 安排 1 轮专项题库训练。", "清理待复盘点后再补一场定向模拟。"))
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("analytics:profile", "analytics:topic:12", "analytics:retrospective:topic:12", "study-plan:active"),
                "把领域回顾转成下周训练重点"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(planService).saveTopicRetrospectiveAction(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(12L),
                org.mockito.ArgumentMatchers.eq("JVM"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.contains("专项题库训练"),
                org.mockito.ArgumentMatchers.contains("领域回顾专项"),
                org.mockito.ArgumentMatchers.contains("画像分仍处在较低区间"),
                org.mockito.ArgumentMatchers.eq("/analytics?topic=12"));
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式训练任务"));
    }

    @Test
    void createRun_interviewReviewUsesLatestInterviewContext() throws Exception {
        when(interviewService.trendData(1L, 1)).thenReturn(List.of(InterviewHistoryVO.builder()
                .sessionId(77L)
                .direction("Java 后端")
                .build()));
        when(interviewService.detail(1L, 77L)).thenReturn(InterviewDetailVO.builder()
                .sessionId(77L)
                .direction("Java 后端")
                .jobRole("后端开发")
                .techStack("Spring, MySQL")
                .records(List.of(
                        InterviewDetailVO.InterviewRecordVO.builder()
                                .questionId(101L)
                                .questionTitle("Redis 缓存一致性")
                                .isLowScore(true)
                                .reviewSummary("缓存一致性回答不完整，需要补双写与失效策略。")
                                .weakPointTags(List.of("Redis", "缓存一致性"))
                                .build(),
                        InterviewDetailVO.InterviewRecordVO.builder()
                                .questionId(102L)
                                .questionTitle("MySQL 索引")
                                .isLowScore(true)
                                .comment("索引失效条件回答不完整。")
                                .weakPointTags(List.of("MySQL", "索引"))
                                .build()))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "interview_review",
                "interview",
                List.of("interview:latest"),
                null));

        assertTrue(result.getSummary().contains("2 道低分题"));
        assertEquals("/interview/detail/77", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getApprovalPayloadJson());
        assertEquals("Java 后端", payload.get("focusDirection").asText());
        assertEquals("后端开发", payload.get("targetRole").asText());
        assertEquals("Spring, MySQL", payload.get("techStack").asText());
    }

    @Test
    void createRun_interviewReviewUsesCopilotRealtimePostReviewContext() {
        when(interviewCopilotRealtimeService.detail(1L, 45L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(45L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .status("completed")
                .endedAt(LocalDateTime.of(2026, 6, 2, 11, 30))
                .postInterviewReview(CopilotRealtimeSessionVO.PostInterviewReviewVO.builder()
                        .summary("字节跳动 / Java 后端开发 的实时阶段已结束，下一步适合直接转入面后复盘。")
                        .weakPoints(List.of("现场备注已记录，但还没有沉淀成正式训练动作。"))
                        .recommendedActions(List.of(
                                "先把本轮实时阶段转成面后复盘 run，整理追问、卡壳点和表达缺口。",
                                "结合 Java 后端开发 岗位目标，决定是否刷新下一轮训练计划。"))
                        .build())
                .build());
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .suggestedFocus("项目表达")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "interview_review",
                "interview_live",
                List.of("interview:copilot-realtime:45", "analytics:profile", "study-plan:active"),
                "把现场追问整理成下一轮训练动作"));

        assertTrue(result.getSummary().contains("实时阶段已结束"));
        assertEquals("/interview", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("实时阶段已经结束")));
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));
    }

    @Test
    void createRun_applicationStrategistUsesApplicationContext() {
        when(jobApplicationService.detail(1L, 6L)).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .status("interview")
                .matchScore(new BigDecimal("84"))
                .missingKeywords(List.of("Redis", "Kafka"))
                .nextStepSuggestion("下周三一面前先补缓存一致性和消息队列。")
                .reviewSuggestion("把最近一轮项目深挖反馈同步到下一轮准备清单。")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "application_strategist",
                "applications",
                List.of("application:6"),
                "优先推进下周的一面"));

        assertTrue(result.getSummary().contains("字节跳动"));
        assertEquals("/applications/6", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("优先推进下周的一面")));
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));
        assertNotNull(result.getApprovalSummary());
    }

    @Test
    void createRun_applicationStrategistUsesApplicationBoardContext() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(3L)
                        .company("携程")
                        .jobTitle("Java 开发")
                        .status("saved")
                        .matchScore(new BigDecimal("88"))
                        .missingKeywords(List.of("Spring Cloud"))
                        .nextStepSuggestion("先决定投递时间。")
                        .build(),
                JobApplicationVO.builder()
                        .id(8L)
                        .company("字节跳动")
                        .jobTitle("后端开发")
                        .status("interview")
                        .matchScore(new BigDecimal("76"))
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .nextStepSuggestion("下周一面前先补缓存一致性和 MQ 场景。")
                        .reviewSuggestion("把上一轮面试反馈同步到项目追问清单。")
                        .build(),
                JobApplicationVO.builder()
                        .id(5L)
                        .company("美团")
                        .jobTitle("平台开发")
                        .status("applied")
                        .matchScore(new BigDecimal("82"))
                        .nextStepSuggestion("等待笔试通知。")
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "application_strategist",
                "applications",
                List.of("application:board"),
                "帮我看今天先推进哪条投递"));

        assertTrue(result.getSummary().contains("共有 3 条岗位记录"));
        assertTrue(result.getSummary().contains("进行中 2 条"));
        assertTrue(result.getSummary().contains("字节跳动"));
        assertEquals("/applications/8", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("进行中岗位有 2 条")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面试中")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));
    }

    @Test
    void createRun_coordinatorUsesDashboardOverviewContext() {
        WeakPointVO weakPoint = new WeakPointVO();
        weakPoint.setCategoryName("系统设计");
        weakPoint.setWrongCount(3);
        when(dashboardService.overview()).thenReturn(DashboardOverviewVO.builder()
                .reviewDebtCount(4)
                .studyStreak(6)
                .weakPoints(List.of(weakPoint))
                .applicationSummary(DashboardOverviewVO.ApplicationSummary.builder()
                        .activeCount(2)
                        .actionPath("/applications")
                        .build())
                .nextAction(NextActionVO.builder()
                        .key("review_debt")
                        .title("清理今日复习债务")
                        .description("先处理到期待复盘和低分点。")
                        .path("/review")
                        .reason("还有 4 项待巩固内容没有消化。")
                        .priority("P1")
                        .build())
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "dashboard",
                List.of("dashboard:overview"),
                "帮我统筹今天的工作台动作"));

        assertEquals("/review", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("清理今日复习债务")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("4 项待巩固")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("2 条进行中投递")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("系统设计")));
    }

    @Test
    void createRun_coordinatorUsesApplicationBoardFocusWhenDashboardMissing() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(11L)
                        .company("小红书")
                        .jobTitle("后端工程师")
                        .status("written")
                        .matchScore(new BigDecimal("79"))
                        .nextStepSuggestion("先完成在线作业，再准备项目细节。")
                        .build(),
                JobApplicationVO.builder()
                        .id(4L)
                        .company("滴滴")
                        .jobTitle("Java 开发")
                        .status("saved")
                        .matchScore(new BigDecimal("91"))
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "applications",
                List.of("application:board"),
                "今天先处理投递还是训练"));

        assertEquals("/applications/11", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("投递看板当前最值得推进的是 小红书 后端工程师")));
    }

    @Test
    void createRun_realtimeCopilotExposesTimelineAndProviderGating() {
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of(
                UserProviderConfigItemVO.builder()
                        .scope("llm")
                        .label("主模型")
                        .status("ready")
                        .statusMessage("配置完整，可供对应能力使用。")
                        .build(),
                UserProviderConfigItemVO.builder()
                        .scope("asr")
                        .label("语音识别")
                        .status("ready")
                        .statusMessage("配置完整，可供对应能力使用。")
                        .build(),
                UserProviderConfigItemVO.builder()
                        .scope("search")
                        .label("联网搜索")
                        .status("missing")
                        .statusMessage("还没有保存这类配置。")
                        .build(),
                UserProviderConfigItemVO.builder()
                        .scope("voiceprint")
                        .label("声纹识别")
                        .status("saved")
                        .statusMessage("配置已保存，启用后可供对应能力使用。")
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("resume:latest", "settings:providers"),
                "先做一轮会前准备"));

        assertEquals("blocked", result.getProviderGateStatus());
        assertTrue(result.getProviderGateSummary().contains("关键 provider"));
        assertTrue(result.getProviderGates().stream().anyMatch(item -> "search".equals(item.getScope()) && "missing".equals(item.getStatus())));
        assertEquals("/settings", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("补齐") && item.contains("联网搜索")));
        assertTrue(result.getTimeline().stream().anyMatch(item -> "request_received".equals(item.getKey())));
        assertTrue(result.getTimeline().stream().anyMatch(item -> "next_action".equals(item.getKey())));
        assertEquals("not_required", result.getApprovalStage());
    }

    @Test
    void createRun_jobPrepUsesProviderSettingsContextForDegradedRecommendations() {
        when(userProviderConfigService.listCurrentUserConfigs()).thenReturn(List.of(
                UserProviderConfigItemVO.builder()
                        .scope("llm")
                        .label("主模型")
                        .status("ready")
                        .statusMessage("配置完整，可供对应能力使用。")
                        .build(),
                UserProviderConfigItemVO.builder()
                        .scope("search")
                        .label("联网搜索")
                        .status("missing")
                        .statusMessage("还没有保存这类配置。")
                        .build()));
        when(jobApplicationService.detail(1L, 6L)).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .missingKeywords(List.of("Redis", "Kafka"))
                .nextStepSuggestion("先准备缓存和消息队列相关案例。")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "job_prep",
                "applications",
                List.of("application:6", "settings:providers"),
                "准备一面"));

        assertEquals("degraded", result.getProviderGateStatus());
        assertEquals("/interview", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("联网搜索") && item.contains("降级")));
    }

    @Test
    void approveRun_persistsJobPrepDraftViaDomainService() {
        when(jobApplicationService.detail(1L, 6L)).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .resumeFileId(9L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .jdText("负责 Java、Redis 和 Kafka 相关服务建设")
                .missingKeywords(List.of("Redis", "Kafka"))
                .nextStepSuggestion("先补 Redis 和 Kafka。")
                .reviewSuggestion("准备项目案例。")
                .build());
        when(interviewJobPrepService.createSession(any(), any())).thenReturn(JobPrepSessionVO.builder()
                .id(301L)
                .applicationId(6L)
                .resumeFileId(9L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .jdText("负责 Java、Redis 和 Kafka 相关服务建设")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "job_prep",
                "applications",
                List.of("application:6"),
                "准备下周一面"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(interviewJobPrepService).createSession(any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式 JD 备面草案"));
    }

    @Test
    void approveRun_persistsApplicationStrategyViaDomainService() {
        when(jobApplicationService.detail(1L, 6L)).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .status("interview")
                .missingKeywords(List.of("Redis", "Kafka"))
                .nextStepSuggestion("下周三一面前先补缓存一致性和消息队列。")
                .reviewSuggestion("把最近一轮项目深挖反馈同步到下一轮准备清单。")
                .build());
        when(jobApplicationService.saveStrategyDraft(any(), any(), any(), any())).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "application_strategist",
                "applications",
                List.of("application:6"),
                "优先推进下周的一面"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(jobApplicationService).saveStrategyDraft(any(), any(), any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("投递策略草案"));
        assertTrue(approved.getExecutionSummary().contains("字节跳动"));
    }

    @Test
    void approveRun_persistsResumeFollowUpDraftViaDomainService() {
        when(jobApplicationService.detail(1L, 6L)).thenReturn(JobApplicationVO.builder()
                .id(6L)
                .resumeFileId(9L)
                .company("字节跳动")
                .jobTitle("Java 后端开发")
                .missingKeywords(List.of("Redis", "Kafka"))
                .build());
        when(resumeService.saveFollowUpDraft(any(), any(), any(), any())).thenReturn(ResumeFileVO.builder()
                .id(9L)
                .title("Java 后端简历")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "resume_coach",
                "applications",
                List.of("application:6"),
                "把项目亮点改得更适合一面深挖"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(resumeService).saveFollowUpDraft(any(), any(), any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("简历追问草稿"));
        assertTrue(approved.getExecutionSummary().contains("Java 后端简历"));
    }

    @Test
    void approveRun_persistsRecordingReviewAsFormalTrainingAction() {
        when(interviewRecordingReviewService.detail(1L, 55L)).thenReturn(RecordingReviewSessionVO.builder()
                .id(55L)
                .direction("Java 后端")
                .jobRole("后端开发")
                .overallScore(new BigDecimal("61"))
                .weakPoints(List.of("表达结构", "项目案例支撑"))
                .suggestedActions(List.of("先回听薄弱片段", "再补 1 次专项模拟"))
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "recording_review",
                "recording_review",
                List.of("interview:recording-review:55", "study-plan:active"),
                "把这次复盘变成今天的训练动作"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(planService).saveRecordingReviewAction(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(55L),
                org.mockito.ArgumentMatchers.eq("Java 后端"),
                org.mockito.ArgumentMatchers.eq("后端开发"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.contains("录音复盘专项"),
                org.mockito.ArgumentMatchers.contains("表达结构"),
                org.mockito.ArgumentMatchers.eq("/interview?recordingReview=55"));
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式训练任务"));
    }

    private AgentRunCreateRequest request(String agentType, String triggerSource, List<String> contextRefs, String prompt) {
        AgentRunCreateRequest request = new AgentRunCreateRequest();
        request.setAgentType(agentType);
        request.setTriggerSource(triggerSource);
        request.setContextRefs(contextRefs);
        request.setStreamMode("sync");
        request.setUserPrompt(prompt);
        return request;
    }
}
