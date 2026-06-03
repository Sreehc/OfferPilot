package com.offerpilot.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
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
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.NextActionVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.service.InterviewCopilotPrepService;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
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
    private InterviewCopilotPrepService interviewCopilotPrepService;
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
        lenient().doAnswer(invocation -> {
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
    void listRuns_appliesAgentStatusTriggerApprovalAndProviderFilters() {
        AgentRun matchedRun = new AgentRun();
        matchedRun.setId(2001L);
        matchedRun.setUserId(1L);
        matchedRun.setAgentType("realtime_copilot");
        matchedRun.setTriggerSource("interview_live");
        matchedRun.setStatus("pending_approval");
        matchedRun.setRequiresApproval(1);
        matchedRun.setTitle("实时 Copilot 代理");
        matchedRun.setSummary("这条 run 应命中 blocked provider 过滤。");
        matchedRun.setContextRefsJson("[\"interview:copilot-realtime:91\"]");
        matchedRun.setNextActionPath("/interview");
        matchedRun.setResultPayloadJson("{\"recommendations\":[],\"checkpoints\":[]}");
        matchedRun.setUpdateTime(LocalDateTime.of(2026, 6, 3, 9, 0));

        AgentRun ignoredRun = new AgentRun();
        ignoredRun.setId(2002L);
        ignoredRun.setUserId(1L);
        ignoredRun.setAgentType("job_prep");
        ignoredRun.setTriggerSource("interview");
        ignoredRun.setStatus("pending_approval");
        ignoredRun.setRequiresApproval(1);
        ignoredRun.setTitle("JD 备面代理");
        ignoredRun.setSummary("这条 run 只会是 degraded，不应命中过滤条件。");
        ignoredRun.setContextRefsJson("[]");
        ignoredRun.setResultPayloadJson("{\"recommendations\":[],\"checkpoints\":[]}");
        ignoredRun.setUpdateTime(LocalDateTime.of(2026, 6, 3, 8, 0));

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

        when(agentRunMapper.selectList(any())).thenReturn(List.of(matchedRun, ignoredRun));

        List<AgentRunVO> result = agentRunService.listRuns(
                1L,
                "",
                "pending_approval",
                "",
                "waiting",
                "blocked");

        assertEquals(1, result.size());
        assertEquals("realtime_copilot", result.get(0).getAgentType());
        assertEquals("pending_approval", result.get(0).getStatus());
        assertEquals("interview_live", result.get(0).getTriggerSource());
        assertEquals("waiting", result.get(0).getApprovalStage());
        assertEquals("blocked", result.get(0).getProviderGateStatus());
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
        assertEquals("/analytics?topic=12", result.getNextActionPath());
        assertEquals("前往主题画像", result.getNextActionLabel());
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
        assertEquals("/analytics?topic=12&retrospective=1", result.getNextActionPath());
        assertEquals("前往领域回顾", result.getNextActionLabel());
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
    void createRun_rejectsUnsupportedAgentType() {
        BusinessException ex = assertThrows(BusinessException.class, () -> agentRunService.createRun(1L, request(
                "unknown_agent",
                "manual",
                List.of(),
                "test")));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("unsupported agentType"));
    }

    @Test
    void createRun_rejectsUnsupportedTriggerSource() {
        BusinessException ex = assertThrows(BusinessException.class, () -> agentRunService.createRun(1L, request(
                "coordinator",
                "unknown_source",
                List.of(),
                "test")));

        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("unsupported triggerSource"));
    }

    @Test
    void createRun_studyPlannerUsesWeakTopicsContext() throws Exception {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .recommendedDifficulty("medium")
                .suggestedFocus("JVM")
                .weakCategories(List.of("JVM", "系统设计"))
                .categoryAbilities(List.of(
                        CategoryAbilityVO.builder()
                                .categoryId(12L)
                                .categoryName("JVM")
                                .abilityScore(52D)
                                .isWeak(true)
                                .recommendedDifficulty("medium")
                                .build(),
                        CategoryAbilityVO.builder()
                                .categoryId(18L)
                                .categoryName("系统设计")
                                .abilityScore(57D)
                                .isWeak(true)
                                .recommendedDifficulty("hard")
                                .build(),
                        CategoryAbilityVO.builder()
                                .categoryId(5L)
                                .categoryName("Redis")
                                .abilityScore(68D)
                                .isWeak(false)
                                .recommendedDifficulty("medium")
                                .build()))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("analytics:profile", "analytics:weak-topics"),
                "按当前弱项生成下轮训练"));

        assertTrue(result.getSummary().contains("JVM"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("弱项主题优先级")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("JVM（52")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("系统设计（57")));
        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getApprovalPayloadJson());
        assertEquals("JVM", payload.get("focusDirection").asText());
    }

    @Test
    void createRun_studyPlannerUsesApplicationBoardFocus() {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .suggestedFocus("系统设计")
                .recommendedDifficulty("medium")
                .build());
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(5L)
                        .resumeFileId(12L)
                        .company("字节跳动")
                        .jobTitle("后端开发")
                        .status("interview")
                        .matchScore(new BigDecimal("81"))
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .nextStepSuggestion("下周一面前先补缓存一致性和 MQ 场景。")
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "study_planner",
                "analytics",
                List.of("study-plan:active", "analytics:profile", "application:board"),
                "把训练动作和优先投递岗位对齐"));

        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("当前优先投递岗位")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("MQ 场景")));
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
                org.mockito.ArgumentMatchers.eq("/analytics?topic=12&retrospective=1"));
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
        assertEquals("save_interview_review_action", result.getApprovalActionType());

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunMapper).insert(captor.capture());
        JsonNode payload = objectMapper.readTree(captor.getValue().getApprovalPayloadJson());
        assertEquals("Java 后端", payload.get("focusDirection").asText());
        assertEquals("后端开发", payload.get("targetRole").asText());
        assertEquals("Spring, MySQL", payload.get("techStack").asText());
    }

    @Test
    void approveRun_persistsInterviewReviewAsFormalTrainingAction() {
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

        AgentRunVO created = agentRunService.createRun(1L, request(
                "interview_review",
                "interview",
                List.of("interview:session:77"),
                "把低分题转成正式训练动作"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(planService).saveInterviewReviewAction(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(77L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("Java 后端"),
                org.mockito.ArgumentMatchers.eq("后端开发"),
                org.mockito.ArgumentMatchers.eq("Spring, MySQL"),
                org.mockito.ArgumentMatchers.contains("面试复盘专项"),
                org.mockito.ArgumentMatchers.contains("Redis"),
                org.mockito.ArgumentMatchers.eq("/interview/detail/77"));
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式训练任务"));
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
        assertEquals("/interview?workspace=copilot-live&copilotRealtimeSessionId=45", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("实时阶段已经结束")));
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));
    }

    @Test
    void createRun_interviewReviewUsesLatestCopilotRealtimeContext() {
        when(interviewCopilotRealtimeService.latest(1L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(46L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .status("completed")
                .endedAt(LocalDateTime.of(2026, 6, 2, 19, 45))
                .postInterviewReview(CopilotRealtimeSessionVO.PostInterviewReviewVO.builder()
                        .summary("美团 / 资深 Java 工程师 的实时阶段已结束，下一步适合直接转入面后复盘。")
                        .weakPoints(List.of("现场备注已记录，但还没有沉淀成正式训练动作。"))
                        .recommendedActions(List.of(
                                "先把本轮实时阶段转成面后复盘 run，整理追问、卡壳点和表达缺口。",
                                "结合 资深 Java 工程师 岗位目标，决定是否刷新下一轮训练计划。"))
                        .build())
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "interview_review",
                "interview_live",
                List.of("interview:copilot-realtime"),
                "直接消费最近一次实时会话"));

        assertTrue(result.getSummary().contains("实时阶段已结束"));
        assertEquals("/interview?workspace=copilot-live&copilotRealtimeSessionId=46", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("直接消费最近一次实时会话")));
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
    void createRun_coordinatorUsesWeakTopicsContext() {
        when(analyticsService.getAbilityProfile(1L)).thenReturn(AbilityProfileVO.builder()
                .suggestedFocus("系统设计")
                .weakCategories(List.of("系统设计", "JVM"))
                .categoryAbilities(List.of(
                        CategoryAbilityVO.builder()
                                .categoryId(18L)
                                .categoryName("系统设计")
                                .abilityScore(49D)
                                .isWeak(true)
                                .recommendedDifficulty("hard")
                                .build(),
                        CategoryAbilityVO.builder()
                                .categoryId(12L)
                                .categoryName("JVM")
                                .abilityScore(56D)
                                .isWeak(true)
                                .recommendedDifficulty("medium")
                                .build()))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "analytics",
                List.of("analytics:weak-topics"),
                "先帮我判断今天最该补哪块"));

        assertEquals("/analytics?topic=18", result.getNextActionPath());
        assertEquals("前往主题画像", result.getNextActionLabel());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("长期画像建议先补 系统设计")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("最该收紧的弱项主题是 系统设计")));
    }

    @Test
    void createRun_coordinatorUsesCopilotPrepContext() {
        when(interviewCopilotPrepService.detail(1L, 58L)).thenReturn(CopilotPrepSessionVO.builder()
                .id(58L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .summary("最近一次 Copilot Prep")
                .nextActions(List.of("先把开场提纲压成 60-90 秒口语版，再进入实时阶段。"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "interview_live",
                List.of("interview:copilot-prep:58"),
                "帮我判断现在先进入实时还是继续准备"));

        assertEquals("/interview?workspace=copilot-live&copilotPrepSessionId=58", result.getNextActionPath());
        assertEquals("前往实时 Copilot", result.getNextActionLabel());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Copilot Prep 已整理完成")));
    }

    @Test
    void createRun_realtimeCopilotPrepPhaseDegradesWithoutBlocking() {
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

        assertEquals("degraded", result.getProviderGateStatus());
        assertTrue(result.getProviderGateSummary().contains("关键 provider 已基本就绪"));
        assertTrue(result.getProviderGates().stream().anyMatch(item -> "search".equals(item.getScope()) && "missing".equals(item.getStatus())));
        assertEquals("/interview?workspace=copilot-prep", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("联网搜索") && item.contains("降级")));
        assertTrue(result.getTimeline().stream().anyMatch(item -> "request_received".equals(item.getKey())));
        assertTrue(result.getTimeline().stream().anyMatch(item -> "next_action".equals(item.getKey())));
        assertTrue(result.getTimeline().stream()
                .anyMatch(item -> "request_received".equals(item.getKey()) && "retrieve".equals(item.getStepType())));
        assertTrue(result.getTimeline().stream()
                .anyMatch(item -> "analysis_ready".equals(item.getKey()) && "prepare_realtime".equals(item.getStepType())));
        assertTrue(result.getTimeline().stream()
                .anyMatch(item -> "next_action".equals(item.getKey()) && "prepare_realtime".equals(item.getStepType())));
        assertEquals("前往 Copilot Prep", result.getNextActionLabel());
        assertEquals("not_required", result.getApprovalStage());
    }

    @Test
    void createRun_realtimeCopilotLivePhaseBlocksWithoutRequiredProviders() {
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
                        .build()));
        when(interviewCopilotRealtimeService.detail(1L, 91L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(91L)
                .company("小红书")
                .jobTitle("后端开发")
                .status("live")
                .latestEventSummary("实时连接已建立。")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:copilot-realtime:91", "settings:providers"),
                "继续实时阶段"));

        assertEquals("blocked", result.getProviderGateStatus());
        assertEquals("/settings?tab=providers", result.getNextActionPath());
        assertEquals("前往 Provider 设置", result.getNextActionLabel());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("补齐") && item.contains("联网搜索")));
    }

    @Test
    void createRun_realtimeCopilotUsesLatestCopilotPrepContext() {
        when(interviewCopilotPrepService.latest(1L)).thenReturn(CopilotPrepSessionVO.builder()
                .id(58L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .summary("最近一次 Copilot Prep")
                .openingBrief(List.of("开场先讲最贴近岗位的项目背景和核心职责。"))
                .liveCues(List.of("如果问题很大，先回答结论，再拆为什么和怎么做。"))
                .keyRisks(List.of("Kafka 追问深挖容易暴露准备边界。"))
                .nextActions(List.of("先把开场提纲压成 60-90 秒口语版，再进入实时阶段。"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:copilot-prep"),
                "继续沿最近一次 Prep 进入实时阶段"));

        verify(interviewCopilotPrepService).latest(1L);
        assertTrue(result.getSummary().contains("美团"));
        assertTrue(result.getSummary().contains("资深 Java 工程师"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("开场提纲")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Kafka")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Copilot Prep 压成可口述")));
        assertEquals("/interview?workspace=copilot-live&copilotPrepSessionId=58", result.getNextActionPath());
        assertEquals("前往实时 Copilot", result.getNextActionLabel());
    }

    @Test
    void createRun_realtimeCopilotUsesJobPrepContextForApprovalDraft() {
        when(interviewJobPrepService.detail(1L, 62L)).thenReturn(JobPrepSessionVO.builder()
                .id(62L)
                .applicationId(8L)
                .resumeFileId(15L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .jdText("负责 Java、Redis、Kafka 相关服务建设")
                .nextActions(List.of("先把 JD 备面结果转成开场和追问清单。"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:job-prep:62"),
                "把这轮备面结果整理成会前 Prep"));

        assertTrue(result.getSummary().contains("会前清单"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("开场和追问清单")));
        assertEquals("save_copilot_prep_draft", result.getApprovalActionType());
        assertTrue(Boolean.TRUE.equals(result.getRequiresApproval()));
        assertEquals("waiting", result.getApprovalStage());
    }

    @Test
    void createRun_realtimeCopilotUsesRealtimeSessionContext() {
        when(interviewCopilotRealtimeService.detail(1L, 88L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(88L)
                .company("小红书")
                .jobTitle("资深后端工程师")
                .status("live")
                .providerStatus("degraded")
                .latestEventSummary("面试官正在追问 Redis 双写一致性。")
                .liveChecklist(List.of("先给结论，再补一致性权衡。", "如果继续追问，再落到项目案例。"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:copilot-realtime:88"),
                "根据现场追问调整回答重点"));

        assertTrue(result.getSummary().contains("实时 Copilot 已连接"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis 双写一致性")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("实时检查清单")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("降级模式")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("根据现场追问调整回答重点")));
        assertEquals("/interview?workspace=copilot-live&copilotRealtimeSessionId=88", result.getNextActionPath());
        assertEquals("前往实时 Copilot", result.getNextActionLabel());
    }

    @Test
    void createRun_realtimeCopilotUsesPostReviewActionPathWhenSessionCompleted() {
        when(interviewCopilotRealtimeService.detail(1L, 89L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(89L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .status("completed")
                .postInterviewReview(CopilotRealtimeSessionVO.PostInterviewReviewVO.builder()
                        .summary("美团 / 资深 Java 工程师 的实时阶段已结束，下一步适合直接转入面后复盘。")
                        .recommendedActions(List.of("先把本轮实时阶段转成面后复盘 run。"))
                        .nextActionPath("/agent?agentType=interview_review&triggerSource=interview_live&contextRefs=interview:copilot-realtime:89,analytics:profile,study-plan:active")
                        .build())
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:copilot-realtime:89"),
                "直接收束到面后复盘"));

        assertTrue(result.getSummary().contains("实时阶段已结束"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面后复盘 run")));
        assertTrue(result.getNextActionPath().contains("agentType=interview_review"));
        assertTrue(result.getNextActionPath().contains("interview:copilot-realtime:89"));
        assertEquals("发起面后复盘", result.getNextActionLabel());
    }

    @Test
    void createRun_coordinatorUsesLiveRealtimeContext() {
        when(interviewCopilotRealtimeService.detail(1L, 88L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(88L)
                .company("小红书")
                .jobTitle("资深后端工程师")
                .status("live")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "interview_live",
                List.of("interview:copilot-realtime:88"),
                "帮我判断当前阶段的下一步"));

        assertEquals("/interview?workspace=copilot-live&copilotRealtimeSessionId=88", result.getNextActionPath());
        assertEquals("前往实时 Copilot", result.getNextActionLabel());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("实时 Copilot 当前仍在连接中")));
    }

    @Test
    void createRun_coordinatorUsesRealtimePostReviewActionPathWhenAvailable() {
        when(interviewCopilotRealtimeService.detail(1L, 90L)).thenReturn(CopilotRealtimeSessionVO.builder()
                .id(90L)
                .company("字节跳动")
                .jobTitle("后端开发")
                .status("completed")
                .postInterviewReview(CopilotRealtimeSessionVO.PostInterviewReviewVO.builder()
                        .nextActionPath("/agent?agentType=interview_review&triggerSource=interview_live&contextRefs=interview:copilot-realtime:90,analytics:profile,study-plan:active")
                        .build())
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "coordinator",
                "interview_live",
                List.of("interview:copilot-realtime:90"),
                "帮我决定下一步"));

        assertTrue(result.getNextActionPath().contains("agentType=interview_review"));
        assertTrue(result.getNextActionPath().contains("interview:copilot-realtime:90"));
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
        assertEquals("/interview?workspace=job-prep", result.getNextActionPath());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("联网搜索") && item.contains("降级")));
    }

    @Test
    void createRun_jobPrepUsesApplicationBoardFocus() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(3L)
                        .resumeFileId(8L)
                        .company("携程")
                        .jobTitle("Java 开发")
                        .status("saved")
                        .matchScore(new BigDecimal("88"))
                        .jdText("负责 Spring Boot 服务开发")
                        .missingKeywords(List.of("Spring Cloud"))
                        .nextStepSuggestion("先决定投递时间。")
                        .build(),
                JobApplicationVO.builder()
                        .id(9L)
                        .resumeFileId(12L)
                        .company("字节跳动")
                        .jobTitle("后端开发")
                        .status("interview")
                        .matchScore(new BigDecimal("76"))
                        .jdText("负责 Java、Redis、Kafka 相关服务建设")
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .reviewSuggestion("把上一轮面试反馈同步到项目追问清单。")
                        .nextStepSuggestion("下周一面前先补缓存一致性和 MQ 场景。")
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "job_prep",
                "manual",
                List.of("resume:latest", "application:board"),
                "优先准备下周最可能进入一面的岗位"));

        assertTrue(result.getSummary().contains("字节跳动"));
        assertTrue(result.getSummary().contains("后端开发"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("面试中")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("MQ 场景")));
        assertEquals("save_job_prep_draft", result.getApprovalActionType());
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
    void approveRun_persistsJobPrepDraftFromApplicationBoardFocus() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(4L)
                        .resumeFileId(16L)
                        .company("小红书")
                        .jobTitle("Java 后端")
                        .status("written")
                        .matchScore(new BigDecimal("81"))
                        .jdText("负责 Java、Redis 与分布式服务建设")
                        .missingKeywords(List.of("Redis"))
                        .nextStepSuggestion("先完成笔试，再准备项目细节。")
                        .build()));
        when(interviewJobPrepService.createSession(any(), any())).thenReturn(JobPrepSessionVO.builder()
                .id(401L)
                .applicationId(4L)
                .resumeFileId(16L)
                .company("小红书")
                .jobTitle("Java 后端")
                .jdText("负责 Java、Redis 与分布式服务建设")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "job_prep",
                "manual",
                List.of("resume:latest", "application:board"),
                "优先准备下一场一面"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(interviewJobPrepService).createSession(any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式 JD 备面草案"));
        assertTrue(approved.getExecutionSummary().contains("Java 后端"));
    }

    @Test
    void approveRun_persistsCopilotPrepDraftViaDomainService() {
        when(interviewJobPrepService.detail(1L, 62L)).thenReturn(JobPrepSessionVO.builder()
                .id(62L)
                .applicationId(8L)
                .resumeFileId(15L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .jdText("负责 Java、Redis、Kafka 相关服务建设")
                .nextActions(List.of("先把 JD 备面结果转成开场和追问清单。"))
                .build());
        when(interviewCopilotPrepService.createSession(any(), any())).thenReturn(CopilotPrepSessionVO.builder()
                .id(501L)
                .applicationId(8L)
                .resumeFileId(15L)
                .jobPrepSessionId(62L)
                .company("美团")
                .jobTitle("资深 Java 工程师")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "realtime_copilot",
                "interview_live",
                List.of("interview:job-prep:62"),
                "把这轮备面结果整理成会前 Prep"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(interviewCopilotPrepService).createSession(any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式 Copilot Prep 草案"));
        assertTrue(approved.getExecutionSummary().contains("资深 Java 工程师"));
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
    void approveRun_persistsApplicationStrategyFromApplicationBoardFocus() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(14L)
                        .company("小红书")
                        .jobTitle("后端工程师")
                        .status("interview")
                        .matchScore(new BigDecimal("83"))
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .nextStepSuggestion("下周一面前先补缓存一致性和 MQ 场景。")
                        .reviewSuggestion("把上一轮面试反馈同步到项目追问清单。")
                        .build(),
                JobApplicationVO.builder()
                        .id(3L)
                        .company("携程")
                        .jobTitle("Java 开发")
                        .status("saved")
                        .matchScore(new BigDecimal("90"))
                        .build()));
        when(jobApplicationService.saveStrategyDraft(any(), any(), any(), any())).thenReturn(JobApplicationVO.builder()
                .id(14L)
                .company("小红书")
                .jobTitle("后端工程师")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "application_strategist",
                "applications",
                List.of("application:board"),
                "优先推进最接近一面的岗位"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(jobApplicationService).saveStrategyDraft(any(), org.mockito.ArgumentMatchers.eq(14L), any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("投递策略草案"));
        assertTrue(approved.getExecutionSummary().contains("小红书"));
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
    void createRun_resumeCoachUsesApplicationBoardFocus() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(2L)
                        .resumeFileId(11L)
                        .company("携程")
                        .jobTitle("Java 开发")
                        .status("saved")
                        .matchScore(new BigDecimal("85"))
                        .missingKeywords(List.of("Spring Cloud"))
                        .build(),
                JobApplicationVO.builder()
                        .id(7L)
                        .resumeFileId(15L)
                        .company("字节跳动")
                        .jobTitle("后端开发")
                        .status("interview")
                        .matchScore(new BigDecimal("79"))
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .reviewSuggestion("把上一轮面试反馈同步到项目追问清单。")
                        .build()));

        AgentRunVO result = agentRunService.createRun(1L, request(
                "resume_coach",
                "applications",
                List.of("application:board"),
                "把项目亮点改得更适合下轮深挖"));

        assertTrue(result.getSummary().contains("后端开发"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Redis")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("项目追问清单")));
        assertEquals("save_resume_follow_up_draft", result.getApprovalActionType());
    }

    @Test
    void createRun_resumeCoachAcceptsResumeTriggerSource() {
        when(resumeService.detail(1L, 15L)).thenReturn(ResumeFileVO.builder()
                .id(15L)
                .title("Java 后端简历")
                .summary("3 年 Java 后端开发，负责高并发交易链路。")
                .selfIntro("我会先用交易链路改造作为开场。")
                .skills(List.of("Java", "Spring Boot", "Redis"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "resume_coach",
                "resume",
                List.of("resume:15", "analytics:profile"),
                "围绕当前简历整理下一轮优化动作"));

        assertTrue(result.getSummary().contains("Java 后端简历"));
        assertEquals("resume", result.getTriggerSource());
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Java")));
        assertEquals("save_resume_follow_up_draft", result.getApprovalActionType());
    }

    @Test
    void approveRun_persistsResumeFollowUpDraftFromApplicationBoardFocus() {
        when(jobApplicationService.board(1L)).thenReturn(List.of(
                JobApplicationVO.builder()
                        .id(7L)
                        .resumeFileId(15L)
                        .company("字节跳动")
                        .jobTitle("后端开发")
                        .status("interview")
                        .matchScore(new BigDecimal("79"))
                        .missingKeywords(List.of("Redis", "Kafka"))
                        .build()));
        when(resumeService.saveFollowUpDraft(any(), any(), any(), any())).thenReturn(ResumeFileVO.builder()
                .id(15L)
                .title("后端专项简历")
                .build());

        AgentRunVO created = agentRunService.createRun(1L, request(
                "resume_coach",
                "applications",
                List.of("application:board"),
                "围绕下轮一面优化项目表达"));

        AgentRunVO approved = agentRunService.approveRun(1L, Long.valueOf(String.valueOf(created.getId())), null);

        verify(resumeService).saveFollowUpDraft(any(), any(), any(), any());
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("简历追问草稿"));
        assertTrue(approved.getExecutionSummary().contains("后端专项简历"));
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
                org.mockito.ArgumentMatchers.eq("/interview?workspace=recording-review&recordingReviewSessionId=55"));
        assertEquals("approved", approved.getStatus());
        assertTrue(approved.getExecutionSummary().contains("正式训练任务"));
        assertTrue(approved.getTimeline().stream()
                .anyMatch(item -> "approval_gate".equals(item.getKey()) && "wait_approval".equals(item.getStepType())));
        assertTrue(approved.getTimeline().stream()
                .anyMatch(item -> "execution_result".equals(item.getKey()) && "schedule_review".equals(item.getStepType())));
    }

    @Test
    void createRun_jobPrepUsesLatestContextWhenIdMissing() {
        when(interviewJobPrepService.latest(1L)).thenReturn(JobPrepSessionVO.builder()
                .id(92L)
                .applicationId(6L)
                .resumeFileId(12L)
                .company("字节跳动")
                .jobTitle("后端开发")
                .matchedKeywords(List.of("Java", "Redis"))
                .focusAreas(List.of("先补 Kafka 和消息可靠性。"))
                .resumeTalkingPoints(List.of("围绕项目 A 讲缓存优化和稳定性结果。"))
                .nextActions(List.of("把这次 JD 备面结果转成 3-5 道模拟题。"))
                .summary("最近一次 JD 备面")
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "job_prep",
                "manual",
                List.of("interview:job-prep"),
                "继续沿最近一次备面结果准备下一场一面"));

        verify(interviewJobPrepService).latest(1L);
        assertTrue(result.getSummary().contains("字节跳动"));
        assertTrue(result.getSummary().contains("后端开发"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("Kafka")));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("模拟题")));
        assertEquals("save_job_prep_draft", result.getApprovalActionType());
    }

    @Test
    void createRun_recordingReviewUsesLatestContextWhenIdMissing() {
        when(interviewRecordingReviewService.latest(1L)).thenReturn(RecordingReviewSessionVO.builder()
                .id(77L)
                .direction("系统设计")
                .jobRole("后端开发")
                .overallScore(new BigDecimal("59"))
                .weakPoints(List.of("回答结构", "案例细节"))
                .suggestedActions(List.of("先回听录音片段", "把问题转成专项训练"))
                .build());

        AgentRunVO result = agentRunService.createRun(1L, request(
                "recording_review",
                "recording_review",
                List.of("interview:recording-review", "study-plan:active"),
                "把最近一次真实复盘转成训练动作"));

        verify(interviewRecordingReviewService).latest(1L);
        assertTrue(result.getSummary().contains("录音复盘"));
        assertTrue(result.getRecommendations().stream().anyMatch(item -> item.contains("回答结构")));
        assertEquals("save_recording_review_action", result.getApprovalActionType());
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
