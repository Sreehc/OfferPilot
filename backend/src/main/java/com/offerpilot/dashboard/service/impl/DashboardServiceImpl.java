package com.offerpilot.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.agent.entity.AgentRun;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.application.entity.JobApplication;
import com.offerpilot.application.mapper.JobApplicationMapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.NextActionVO;
import com.offerpilot.dashboard.dto.RecentInterviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.mapper.DashboardMetricsMapper;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.entity.CopilotPrepSession;
import com.offerpilot.interview.entity.CopilotRealtimeSession;
import com.offerpilot.interview.entity.JobPrepSession;
import com.offerpilot.interview.entity.RecordingReviewSession;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_PREFIX = "dashboard:overview:";

    private final DashboardMetricsMapper dashboardMetricsMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AdaptiveService adaptiveService;
    private final OfferPilotProperties props;
    private final JobApplicationMapper jobApplicationMapper;
    private final ReviewLogMapper reviewLogMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final StudyPlanTaskMapper studyPlanTaskMapper;
    private final ResumeFileMapper resumeFileMapper;
    private final AgentRunMapper agentRunMapper;
    private final JobPrepSessionMapper jobPrepSessionMapper;
    private final CopilotPrepSessionMapper copilotPrepSessionMapper;
    private final CopilotRealtimeSessionMapper copilotRealtimeSessionMapper;
    private final RecordingReviewSessionMapper recordingReviewSessionMapper;

    @Override
    public DashboardOverviewVO overview() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        return overview(userId);
    }

    @Override
    public DashboardOverviewVO overview(Long userId) {
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
        StudyPlan activePlan = loadActivePlan(userId);
        List<StudyPlanTask> todayPlanTasks = loadTodayPlanTasks(activePlan);
        List<JobApplication> applications = loadApplications(userId);
        ResumeFile latestResume = loadLatestResume(userId);

        DashboardOverviewVO result = DashboardOverviewVO.builder()
                .learningCount(learningCount)
                .averageScore(averageScore)
                .wrongCount(wrongCount)
                .recentInterviews(recentInterviews)
                .weakPoints(weakPoints)
                .firstVisit(learningCount == 0 && wrongCount == 0)
                .reviewDebtCount((int) defaultLong(dashboardMetricsMapper.countReviewDebt(userId)))
                .studyStreak(resolveStudyStreak(userId))
                .nextAction(resolveNextAction(activePlan, todayPlanTasks, applications, latestResume))
                .applicationSummary(loadApplicationSummary(applications))
                .workflowContinuations(buildWorkflowContinuations(userId))
                .build();

        try {
            AbilityProfileVO profile = adaptiveService.getAbilityProfile(userId);
            result.setWeakCategories(profile.getWeakCategories());
            result.setSuggestedFocus(profile.getSuggestedFocus());
            result.setCategoryAbilities(profile.getCategoryAbilities());
        } catch (Exception e) {
            log.warn("Failed to load adaptive profile for dashboard: {}", e.getMessage());
        }

        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, props.getDashboard().getCacheTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to write dashboard cache: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public void evictCache(Long userId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to evict dashboard cache: {}", e.getMessage());
        }
    }

    private DashboardOverviewVO.ApplicationSummary loadApplicationSummary(List<JobApplication> applications) {
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
        BigDecimal scoreSum = applications.stream()
                .map(JobApplication::getMatchScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int scoreCount = (int) applications.stream()
                .map(JobApplication::getMatchScore)
                .filter(java.util.Objects::nonNull)
                .count();
        BigDecimal avgScore = scoreCount > 0
                ? scoreSum.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP)
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

    private List<JobApplication> loadApplications(Long userId) {
        return jobApplicationMapper.selectList(new LambdaQueryWrapper<JobApplication>()
                .eq(JobApplication::getUserId, userId)
                .orderByDesc(JobApplication::getUpdateTime));
    }

    private ResumeFile loadLatestResume(Long userId) {
        List<ResumeFile> resumes = resumeFileMapper.selectList(new LambdaQueryWrapper<ResumeFile>()
                .eq(ResumeFile::getUserId, userId)
                .orderByDesc(ResumeFile::getUpdateTime)
                .last("LIMIT 1"));
        return resumes.isEmpty() ? null : resumes.get(0);
    }

    private StudyPlan loadActivePlan(Long userId) {
        return studyPlanMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .ne(StudyPlan::getStatus, "archived")
                .orderByDesc(StudyPlan::getUpdateTime)
                .last("LIMIT 1"));
    }

    private List<StudyPlanTask> loadTodayPlanTasks(StudyPlan activePlan) {
        if (activePlan == null || activePlan.getCurrentDay() == null) {
            return List.of();
        }
        return studyPlanTaskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, activePlan.getId())
                .eq(StudyPlanTask::getDayIndex, activePlan.getCurrentDay())
                .orderByAsc(StudyPlanTask::getId));
    }

    private NextActionVO resolveNextAction(
            StudyPlan activePlan,
            List<StudyPlanTask> todayPlanTasks,
            List<JobApplication> applications,
            ResumeFile latestResume) {
        int activeApplicationCount = (int) applications.stream()
                .filter(item -> List.of("applied", "written", "interview").contains(item.getStatus()))
                .count();
        long pendingTodayTaskCount = todayPlanTasks.stream()
                .filter(task -> !"completed".equals(task.getStatus()))
                .count();

        if (latestResume == null) {
            return NextActionVO.builder()
                    .key("upload_resume")
                    .title("先上传简历")
                    .description("先把简历整理出来，再继续项目追问、模拟面试和投递。")
                    .path("/resume#resume-upload")
                    .reason("当前还没有简历，后续训练和求职链路缺少基础资料。")
                    .priority("P0")
                    .build();
        }
        if (activePlan == null) {
            return NextActionVO.builder()
                    .key("generate_plan")
                    .title("先生成训练计划")
                    .description("把接下来几天要练的题目、问答和模拟面试先排好。")
                    .path("/study-plan#plan-builder")
                    .reason("当前还没有学习计划，首页和计划页都缺少统一执行清单。")
                    .priority("P0")
                    .build();
        }
        if (pendingTodayTaskCount > 0) {
            return NextActionVO.builder()
                    .key("complete_today_plan")
                    .title("先完成今天的计划")
                    .description(String.format("今天还有 %d 项计划任务待处理，先把它们推进完。", pendingTodayTaskCount))
                    .path("/study-plan")
                    .reason("当前已有计划且今天仍有未完成任务，应优先回到执行页。")
                    .priority("P0")
                    .build();
        }
        if (activeApplicationCount > 0) {
            return NextActionVO.builder()
                    .key("job_prep")
                    .title("先做一轮 JD 定向备面")
                    .description("围绕正在推进的岗位，先把 JD 要求、简历表达和面试追问拆成一轮正式备面结果。")
                    .path("/interview?workspace=job-prep")
                    .reason("当前有真实投递在推进，优先进入与岗位直接关联的备面工作区，而不是停留在泛化投递列表。")
                    .priority("P1")
                    .build();
        }
        return NextActionVO.builder()
                .key("start_interview")
                .title("安排下一轮模拟面试")
                .description("用一轮新的模拟面试检查这段时间的训练有没有真正转成表达能力。")
                .path("/interview?workspace=mock-interview")
                .reason("当前基础资料和计划都已具备，下一步应回到表达检验。")
                .priority("P2")
                .build();
    }

    private List<DashboardOverviewVO.WorkflowContinuation> buildWorkflowContinuations(Long userId) {
        List<DashboardOverviewVO.WorkflowContinuation> items = new ArrayList<>();

        AgentRun waitingApprovalRun = loadLatestPendingApprovalRun(userId);
        long pendingApprovalCount = countPendingApprovalRuns(userId);
        if (waitingApprovalRun != null && pendingApprovalCount > 0) {
            items.add(DashboardOverviewVO.WorkflowContinuation.builder()
                    .key("agent_approval")
                    .label("处理待审批 Agent")
                    .status("待审批 " + pendingApprovalCount + " 项")
                    .description(firstNonBlank(
                            waitingApprovalRun.getApprovalSummary(),
                            waitingApprovalRun.getSummary(),
                            "当前有待确认的写操作结果，处理后才能正式写回训练、简历或面试链路。"))
                    .path("/agent?runId=" + waitingApprovalRun.getId()
                            + "&listStatus=pending_approval&listApprovalStage=waiting")
                    .tone("amber")
                    .build());
        }

        CopilotRealtimeSession realtimeSession = loadLatestCopilotRealtimeSession(userId);
        if (realtimeSession != null && !"completed".equalsIgnoreCase(realtimeSession.getStatus())) {
            items.add(DashboardOverviewVO.WorkflowContinuation.builder()
                    .key("copilot_realtime")
                    .label(resolveRealtimeContinuationLabel(realtimeSession.getStatus()))
                    .status(resolveRealtimeContinuationStatus(realtimeSession.getStatus()))
                    .description(firstNonBlank(
                            realtimeSession.getLatestEventSummary(),
                            buildRealtimeDescription(realtimeSession),
                            "最近一次实时 Copilot 会话仍可继续，可直接回到实时阶段或恢复连接。"))
                    .path("/interview?workspace=copilot-live&copilotRealtimeSessionId=" + realtimeSession.getId())
                    .tone("violet")
                    .build());
        }

        RecordingReviewSession recordingReview = loadLatestRecordingReviewSession(userId);
        if (recordingReview != null && isRecordingContinuationEligible(recordingReview.getStatus())) {
            items.add(DashboardOverviewVO.WorkflowContinuation.builder()
                    .key("recording_review")
                    .label(resolveRecordingContinuationLabel(recordingReview.getStatus()))
                    .status(resolveRecordingContinuationStatus(recordingReview.getStatus()))
                    .description(firstNonBlank(
                            recordingReview.getSummary(),
                            recordingReview.getStatusMessage(),
                            buildRecordingDescription(recordingReview),
                            "最近一次录音复盘可继续查看转写、弱点和训练建议。"))
                    .path("/interview?workspace=recording-review&recordingReviewSessionId=" + recordingReview.getId())
                    .tone("amber")
                    .build());
        }

        CopilotPrepSession prepSession = loadLatestCopilotPrepSession(userId);
        if (prepSession != null) {
            items.add(DashboardOverviewVO.WorkflowContinuation.builder()
                    .key("copilot_prep")
                    .label("继续 Copilot Prep")
                    .status("会前草案可继续")
                    .description(firstNonBlank(
                            prepSession.getSummary(),
                            buildPrepDescription(prepSession),
                            "最近一次 Copilot Prep 已生成，可以继续整理开场和追问清单。"))
                    .path("/interview?workspace=copilot-prep&copilotPrepSessionId=" + prepSession.getId())
                    .tone("teal")
                    .build());
        }

        JobPrepSession jobPrepSession = loadLatestJobPrepSession(userId);
        if (jobPrepSession != null) {
            items.add(DashboardOverviewVO.WorkflowContinuation.builder()
                    .key("job_prep")
                    .label("继续 JD 备面")
                    .status("结果可继续消费")
                    .description(firstNonBlank(
                            jobPrepSession.getSummary(),
                            buildJobPrepDescription(jobPrepSession),
                            "最近一次 JD 备面结果仍可继续补充和带入后续 Prep。"))
                    .path("/interview?workspace=job-prep&jobPrepSessionId=" + jobPrepSession.getId())
                    .tone("blue")
                    .build());
        }

        return items.stream()
                .collect(java.util.stream.Collectors.toMap(
                        DashboardOverviewVO.WorkflowContinuation::getPath,
                        item -> item,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new))
                .values()
                .stream()
                .limit(4)
                .toList();
    }

    private AgentRun loadLatestPendingApprovalRun(Long userId) {
        return agentRunMapper.selectOne(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .eq(AgentRun::getStatus, "pending_approval")
                .orderByDesc(AgentRun::getUpdateTime)
                .last("LIMIT 1"));
    }

    private long countPendingApprovalRuns(Long userId) {
        return defaultLong(agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRun>()
                .eq(AgentRun::getUserId, userId)
                .eq(AgentRun::getStatus, "pending_approval")));
    }

    private JobPrepSession loadLatestJobPrepSession(Long userId) {
        return jobPrepSessionMapper.selectOne(new LambdaQueryWrapper<JobPrepSession>()
                .eq(JobPrepSession::getUserId, userId)
                .orderByDesc(JobPrepSession::getUpdateTime)
                .last("LIMIT 1"));
    }

    private CopilotPrepSession loadLatestCopilotPrepSession(Long userId) {
        return copilotPrepSessionMapper.selectOne(new LambdaQueryWrapper<CopilotPrepSession>()
                .eq(CopilotPrepSession::getUserId, userId)
                .orderByDesc(CopilotPrepSession::getUpdateTime)
                .last("LIMIT 1"));
    }

    private CopilotRealtimeSession loadLatestCopilotRealtimeSession(Long userId) {
        return copilotRealtimeSessionMapper.selectOne(new LambdaQueryWrapper<CopilotRealtimeSession>()
                .eq(CopilotRealtimeSession::getUserId, userId)
                .orderByDesc(CopilotRealtimeSession::getUpdateTime)
                .last("LIMIT 1"));
    }

    private RecordingReviewSession loadLatestRecordingReviewSession(Long userId) {
        return recordingReviewSessionMapper.selectOne(new LambdaQueryWrapper<RecordingReviewSession>()
                .eq(RecordingReviewSession::getUserId, userId)
                .orderByDesc(RecordingReviewSession::getUpdateTime)
                .last("LIMIT 1"));
    }

    private boolean isRecordingContinuationEligible(String status) {
        return "processing".equalsIgnoreCase(status)
                || "transcribing".equalsIgnoreCase(status)
                || "analyzing".equalsIgnoreCase(status)
                || "ready".equalsIgnoreCase(status)
                || "failed".equalsIgnoreCase(status);
    }

    private String resolveRealtimeContinuationLabel(String status) {
        return switch (normalize(status)) {
            case "live" -> "继续实时 Copilot";
            case "disconnected" -> "恢复实时 Copilot";
            case "awaiting_connection" -> "进入实时 Copilot";
            default -> "查看实时 Copilot";
        };
    }

    private String resolveRealtimeContinuationStatus(String status) {
        return switch (normalize(status)) {
            case "live" -> "实时已连接";
            case "disconnected" -> "连接已中断";
            case "awaiting_connection" -> "待建立连接";
            default -> "可继续";
        };
    }

    private String resolveRecordingContinuationLabel(String status) {
        return switch (normalize(status)) {
            case "processing", "transcribing", "analyzing" -> "查看录音复盘进度";
            case "failed" -> "重试录音复盘";
            default -> "消费录音复盘结果";
        };
    }

    private String resolveRecordingContinuationStatus(String status) {
        return switch (normalize(status)) {
            case "processing" -> "排队处理中";
            case "transcribing" -> "转写处理中";
            case "analyzing" -> "复盘整理中";
            case "failed" -> "处理失败";
            default -> "已完成";
        };
    }

    private String buildRealtimeDescription(CopilotRealtimeSession session) {
        return firstNonBlank(
                joinParts(session.getCompany(), session.getJobTitle()),
                "最近一次实时 Copilot 会话",
                "最近一次实时 Copilot 会话");
    }

    private String buildRecordingDescription(RecordingReviewSession session) {
        String title = firstNonBlank(
                joinParts(session.getDirection(), session.getJobRole()),
                hasAudioInput(session) ? "最近一次录音复盘" : "最近一次 transcript 复盘",
                "最近一次录音复盘");
        if (hasAudioInput(session)) {
            return title;
        }
        return title + "（文字模式）";
    }

    private boolean hasAudioInput(RecordingReviewSession session) {
        return session != null && StringUtils.hasText(session.getAudioUrl());
    }

    private String buildPrepDescription(CopilotPrepSession session) {
        return firstNonBlank(
                joinParts(session.getCompany(), session.getJobTitle()),
                "最近一次 Copilot Prep",
                "最近一次 Copilot Prep");
    }

    private String buildJobPrepDescription(JobPrepSession session) {
        return firstNonBlank(
                joinParts(session.getCompany(), session.getJobTitle()),
                "最近一次 JD 备面",
                "最近一次 JD 备面");
    }

    private String joinParts(String left, String right) {
        String first = StringUtils.hasText(left) ? left.trim() : "";
        String second = StringUtils.hasText(right) ? right.trim() : "";
        if (!first.isEmpty() && !second.isEmpty()) {
            return first + " / " + second;
        }
        return !first.isEmpty() ? first : second;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private int resolveStudyStreak(Long userId) {
        List<ReviewLog> logs = reviewLogMapper.selectList(new LambdaQueryWrapper<ReviewLog>()
                .eq(ReviewLog::getUserId, userId)
                .select(ReviewLog::getCreateTime)
                .orderByDesc(ReviewLog::getCreateTime)
                .last("LIMIT 200"));
        if (logs.isEmpty()) {
            return 0;
        }

        Set<LocalDate> reviewDays = logs.stream()
                .map(ReviewLog::getCreateTime)
                .filter(java.util.Objects::nonNull)
                .map(java.time.LocalDateTime::toLocalDate)
                .collect(java.util.stream.Collectors.toSet());
        if (reviewDays.isEmpty()) {
            return 0;
        }

        LocalDate cursor = LocalDate.now();
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

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
