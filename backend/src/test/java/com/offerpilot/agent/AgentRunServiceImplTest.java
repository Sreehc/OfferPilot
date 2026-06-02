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
import com.offerpilot.agent.vo.AgentRunVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.application.service.JobApplicationService;
import com.offerpilot.application.vo.JobApplicationVO;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import com.offerpilot.plan.service.PlanService;
import com.offerpilot.resume.service.ResumeService;
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
    private InterviewService interviewService;
    @Mock
    private InterviewRecordingReviewService interviewRecordingReviewService;
    @Mock
    private InterviewJobPrepService interviewJobPrepService;
    @Mock
    private ResumeService resumeService;
    @Mock
    private JobApplicationService jobApplicationService;

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
