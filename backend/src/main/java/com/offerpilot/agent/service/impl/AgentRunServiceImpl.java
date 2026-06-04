package com.offerpilot.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.agent.dto.AgentRunCreateRequest;
import com.offerpilot.agent.entity.AgentRun;
import com.offerpilot.agent.entity.AgentType;
import com.offerpilot.agent.entity.StepType;
import com.offerpilot.agent.entity.TriggerSource;
import com.offerpilot.agent.mapper.AgentRunMapper;
import com.offerpilot.agent.service.AgentRunService;
import com.offerpilot.agent.service.UserProviderConfigService;
import com.offerpilot.agent.vo.AgentRunVO;
import com.offerpilot.agent.vo.UserProviderConfigItemVO;
import com.offerpilot.analytics.service.AnalyticsService;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.ProfileTopicRetrospectiveVO;
import com.offerpilot.application.service.JobApplicationService;
import com.offerpilot.application.vo.JobApplicationVO;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.NextActionVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.interview.dto.CopilotPrepSessionCreateRequest;
import com.offerpilot.interview.dto.JobPrepSessionCreateRequest;
import com.offerpilot.interview.service.InterviewCopilotPrepService;
import com.offerpilot.interview.service.InterviewCopilotRealtimeService;
import com.offerpilot.interview.service.InterviewJobPrepService;
import com.offerpilot.interview.service.InterviewRecordingReviewService;
import com.offerpilot.interview.service.InterviewService;
import com.offerpilot.interview.vo.CopilotPrepSessionVO;
import com.offerpilot.interview.vo.CopilotRealtimeSessionVO;
import com.offerpilot.interview.vo.InterviewDetailVO;
import com.offerpilot.interview.vo.InterviewHistoryVO;
import com.offerpilot.interview.vo.JobPrepSessionVO;
import com.offerpilot.interview.vo.RecordingReviewSessionVO;
import com.offerpilot.knowledge.service.KnowledgeService;
import com.offerpilot.knowledge.vo.KnowledgeDocVO;
import com.offerpilot.plan.dto.StudyPlanGenerateRequest;
import com.offerpilot.plan.service.PlanService;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;
import com.offerpilot.question.service.QuestionService;
import com.offerpilot.question.vo.QuestionVO;
import com.offerpilot.resume.service.ResumeService;
import com.offerpilot.resume.vo.ResumeFileVO;
import com.offerpilot.wrong.service.WrongService;
import com.offerpilot.wrong.vo.WrongQuestionVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRunServiceImpl implements AgentRunService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AgentRunMapper agentRunMapper;
    private final ObjectMapper objectMapper;
    private final PlanService planService;
    private final AnalyticsService analyticsService;
    private final DashboardService dashboardService;
    private final InterviewService interviewService;
    private final InterviewRecordingReviewService interviewRecordingReviewService;
    private final InterviewJobPrepService interviewJobPrepService;
    private final InterviewCopilotPrepService interviewCopilotPrepService;
    private final InterviewCopilotRealtimeService interviewCopilotRealtimeService;
    private final ResumeService resumeService;
    private final JobApplicationService jobApplicationService;
    private final UserProviderConfigService userProviderConfigService;
    private final QuestionService questionService;
    private final WrongService wrongService;
    private final KnowledgeService knowledgeService;

    @Override
    @Transactional
    public AgentRunVO createRun(Long userId, AgentRunCreateRequest request) {
        AgentType agentType = requireAgentType(request.getAgentType());
        TriggerSource triggerSource = requireTriggerSource(request.getTriggerSource());
        RunBlueprint blueprint = buildBlueprint(userId, request);
        AgentRun run = new AgentRun();
        run.setUserId(userId);
        run.setAgentType(agentType.value());
        run.setTriggerSource(triggerSource.value());
        run.setStatus(blueprint.requiresApproval() ? "pending_approval" : "completed");
        run.setTitle(blueprint.title());
        run.setSummary(blueprint.summary());
        run.setUserPrompt(trimToNull(request.getUserPrompt()));
        run.setContextRefsJson(writeList(request.getContextRefs()));
        run.setStreamMode(StringUtils.hasText(request.getStreamMode()) ? request.getStreamMode().trim() : "sync");
        run.setResultPayloadJson(writePayload(blueprint.recommendations(), blueprint.checkpoints()));
        run.setNextActionPath(blueprint.nextActionPath());
        run.setRequiresApproval(blueprint.requiresApproval() ? 1 : 0);
        run.setApprovalActionType(blueprint.approvalActionType());
        run.setApprovalSummary(blueprint.approvalSummary());
        run.setApprovalPayloadJson(blueprint.approvalPayloadJson());
        agentRunMapper.insert(run);
        return buildVo(run);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentRunVO> listRuns(Long userId, String agentType, String status, String triggerSource,
                                     String approvalStage, String providerGateStatus) {
        return agentRunMapper.selectList(new LambdaQueryWrapper<AgentRun>()
                        .eq(AgentRun::getUserId, userId)
                        .eq(StringUtils.hasText(agentType), AgentRun::getAgentType, trimToNull(agentType))
                        .eq(StringUtils.hasText(status), AgentRun::getStatus, trimToNull(status))
                        .eq(StringUtils.hasText(triggerSource), AgentRun::getTriggerSource, trimToNull(triggerSource))
                        .orderByDesc(AgentRun::getUpdateTime)
                        .orderByDesc(AgentRun::getId))
                .stream()
                .filter(run -> matchesRunFilter(run, agentType, status, triggerSource, approvalStage, providerGateStatus))
                .map(this::buildVo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentRunVO detail(Long userId, Long runId) {
        return buildVo(getOwnedRun(userId, runId));
    }

    @Override
    @Transactional
    public AgentRunVO approveRun(Long userId, Long runId, String note) {
        AgentRun run = getOwnedRun(userId, runId);
        if (!Integer.valueOf(1).equals(run.getRequiresApproval())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前 run 不需要审批");
        }
        if (!"pending_approval".equals(run.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前 run 不处于待审批状态");
        }

        ExecutionResult result = executeApprovalAction(userId, run);
        run.setStatus("approved");
        run.setDecisionNote(trimToNull(note));
        run.setExecutionResultJson(writeObject(result, "{}"));
        if (result != null && StringUtils.hasText(result.nextActionPath())) {
            run.setNextActionPath(result.nextActionPath());
        }
        agentRunMapper.updateById(run);
        return buildVo(run);
    }

    @Override
    @Transactional
    public AgentRunVO rejectRun(Long userId, Long runId, String note) {
        AgentRun run = getOwnedRun(userId, runId);
        if (!"pending_approval".equals(run.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前 run 不处于待审批状态");
        }
        run.setStatus("rejected");
        run.setDecisionNote(trimToNull(note));
        agentRunMapper.updateById(run);
        return buildVo(run);
    }

    @Override
    @Transactional
    public AgentRunVO cancelRun(Long userId, Long runId, String note) {
        AgentRun run = getOwnedRun(userId, runId);
        if (List.of("approved", "rejected", "canceled").contains(run.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前 run 已结束，不能再取消");
        }
        run.setStatus("canceled");
        run.setDecisionNote(trimToNull(note));
        agentRunMapper.updateById(run);
        return buildVo(run);
    }

    private AgentRun getOwnedRun(Long userId, Long runId) {
        AgentRun run = agentRunMapper.selectById(runId);
        if (run == null || !run.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "agent run not found");
        }
        return run;
    }

    private RunBlueprint buildBlueprint(Long userId, AgentRunCreateRequest request) {
        AgentType agentType = requireAgentType(request.getAgentType());
        List<String> contextRefs = request.getContextRefs() == null ? List.of() : request.getContextRefs().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        String prompt = trimToNull(request.getUserPrompt());
        ContextSnapshot snapshot = resolveContextSnapshot(userId, contextRefs);

        return switch (agentType) {
            case STUDY_PLANNER -> buildStudyPlannerBlueprint(contextRefs, prompt, snapshot);
            case JOB_PREP -> buildJobPrepBlueprint(contextRefs, prompt, snapshot);
            case RECORDING_REVIEW -> buildRecordingReviewBlueprint(contextRefs, prompt, snapshot);
            case RESUME_COACH -> buildResumeCoachBlueprint(contextRefs, prompt, snapshot);
            case APPLICATION_STRATEGIST -> buildApplicationStrategistBlueprint(contextRefs, prompt, snapshot);
            case INTERVIEW_REVIEW -> buildInterviewReviewBlueprint(contextRefs, prompt, snapshot);
            case REALTIME_COPILOT -> buildRealtimeCopilotBlueprint(contextRefs, prompt, snapshot);
            case COORDINATOR -> buildCoordinatorBlueprint(contextRefs, prompt, snapshot);
        };
    }

    private AgentType requireAgentType(String raw) {
        AgentType agentType = AgentType.fromValue(raw);
        if (agentType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported agentType: " + defaultText(raw, ""));
        }
        return agentType;
    }

    private TriggerSource requireTriggerSource(String raw) {
        TriggerSource triggerSource = TriggerSource.fromValue(raw);
        if (triggerSource == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported triggerSource: " + defaultText(raw, ""));
        }
        return triggerSource;
    }

    private RunBlueprint buildStudyPlannerBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        StudyPlanPayload payload = resolveStudyPlanPayload(snapshot);
        TopicRetrospectiveActionPayload retrospectivePayload = resolveTopicRetrospectiveActionPayload(snapshot, payload);
        String focus = firstNonBlank(
                snapshot.topicDetail() == null ? null : snapshot.topicDetail().getCategoryName(),
                snapshot.weakTopicSnapshot() == null ? null : snapshot.weakTopicSnapshot().focusTopicName(),
                snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus(),
                payload.focusDirection());
        String summary = StringUtils.hasText(focus)
                ? "已围绕 " + focus + " 的长期画像、复盘结果和当前上下文生成下一轮训练建议。"
                : "已根据当前画像、复盘节奏和上下文生成下一轮训练建议。";
        List<String> recommendations = mergeRecommendations(
                studyPlannerRecommendations(snapshot),
                prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入下一轮计划排序。") : List.of(),
                contextRefsText(contextRefs, "优先参考这些上下文："));
        boolean useRetrospectiveAction = retrospectivePayload != null;
        return blueprint(
                "学习计划代理",
                summary,
                recommendations,
                List.of("确认主攻方向", "生成可执行任务", "审批通过后写回正式训练计划"),
                resolveRunNextActionPath(resolveStudyPlannerNextActionPath(snapshot), "study_planner", snapshot),
                true,
                useRetrospectiveAction ? "save_topic_retrospective_action" : "refresh_study_plan",
                useRetrospectiveAction
                        ? "审批通过后会把这份领域回顾结论写成当前计划里的正式训练动作，方便按风险和下一步动作直接执行。"
                        : StringUtils.hasText(focus)
                                ? "审批通过后会围绕 " + focus + " 生成或刷新学习计划，把这轮建议落成正式训练动作。"
                                : "审批通过后会生成或刷新当前学习计划，把这轮建议落成正式训练动作。",
                useRetrospectiveAction ? writeObject(retrospectivePayload, "{}") : writeObject(payload, "{}"));
    }

    private RunBlueprint buildJobPrepBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        JobPrepSessionVO jobPrepSession = snapshot.jobPrepSession();
        JobApplicationVO application = snapshot.application();
        JobApplicationVO boardFocusApplication = snapshot.applicationBoard() == null ? null : snapshot.applicationBoard().focusApplication();
        String company = firstNonBlank(
                jobPrepSession == null ? null : jobPrepSession.getCompany(),
                application == null ? null : application.getCompany(),
                boardFocusApplication == null ? null : boardFocusApplication.getCompany());
        String jobTitle = firstNonBlank(
                jobPrepSession == null ? null : jobPrepSession.getJobTitle(),
                application == null ? null : application.getJobTitle(),
                boardFocusApplication == null ? null : boardFocusApplication.getJobTitle());
        String summary;
        List<String> recommendations;
        if (jobPrepSession != null) {
            summary = "已根据 " + defaultText(company, "目标公司") + " " + defaultText(jobTitle, "目标岗位") + " 的备面结果整理缺口和模拟前动作。";
            recommendations = mergeRecommendations(
                    jobPrepSession.getNextActions(),
                    jobPrepSession.getFocusAreas(),
                    jobPrepSession.getResumeTalkingPoints(),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到 JD 备面草案。") : List.of(),
                    contextRefsText(contextRefs, "当前引用的上下文："));
        } else if (application != null) {
            summary = "已根据 " + defaultText(application.getCompany(), "目标公司") + " " + defaultText(application.getJobTitle(), "目标岗位") + " 的 JD 分析整理备面重点。";
            recommendations = mergeRecommendations(
                    nullSafeList(application.getMissingKeywords()).isEmpty()
                            ? List.of("当前 JD 缺口不明显，可以把重点放到项目表达和追问准备。")
                            : List.of("优先补齐 JD 缺口：" + joinLimited(application.getMissingKeywords(), 3, "、") + "。"),
                    StringUtils.hasText(application.getReviewSuggestion()) ? List.of(application.getReviewSuggestion()) : List.of(),
                    StringUtils.hasText(application.getNextStepSuggestion()) ? List.of(application.getNextStepSuggestion()) : List.of(),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入这轮备面优先级。") : List.of(),
                    contextRefsText(contextRefs, "当前引用的上下文："));
        } else if (boardFocusApplication != null) {
            summary = "已根据当前投递看板整理下一场优先备面的岗位。当前建议先准备 "
                    + defaultText(boardFocusApplication.getCompany(), "目标公司")
                    + " "
                    + defaultText(boardFocusApplication.getJobTitle(), "目标岗位")
                    + "。";
            recommendations = mergeRecommendations(
                    List.of("当前看板焦点岗位状态：" + applicationStatusLabel(boardFocusApplication.getStatus()) + "。"),
                    nullSafeList(boardFocusApplication.getMissingKeywords()).isEmpty()
                            ? List.of("当前 JD 缺口不明显，可以把重点放到项目表达和高频追问准备。")
                            : List.of("优先补齐 JD 缺口：" + joinLimited(boardFocusApplication.getMissingKeywords(), 3, "、") + "。"),
                    StringUtils.hasText(boardFocusApplication.getReviewSuggestion()) ? List.of(boardFocusApplication.getReviewSuggestion()) : List.of(),
                    StringUtils.hasText(boardFocusApplication.getNextStepSuggestion()) ? List.of(boardFocusApplication.getNextStepSuggestion()) : List.of(),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入这轮备面优先级。") : List.of(),
                    contextRefsText(contextRefs, "当前引用的上下文："));
        } else {
            summary = "已整理这次岗位准备的重点缺口、项目表达和模拟前动作。";
            recommendations = mergeRecommendations(
                    List.of("先完成 JD 备面，再把结果带入一轮模拟面试。", "围绕缺口关键词准备 1 个项目例子和 1 个原理解释。"),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到备面清单。") : List.of(),
                    contextRefsText(contextRefs, "当前引用的上下文："));
        }
        return blueprint(
                "JD 备面代理",
                summary,
                mergeRecommendations(
                        recommendations,
                        providerContextRecommendations("job_prep", snapshot)),
                List.of("整理 JD 缺口", "准备项目表达", "启动模拟或投递动作"),
                resolveRunNextActionPath(resolveJobPrepWorkspacePath(snapshot), "job_prep", snapshot),
                true,
                "save_job_prep_draft",
                StringUtils.hasText(jobTitle)
                        ? "审批通过后会把这份 " + jobTitle + " 的 JD 备面建议确认为正式草案，方便继续在面试页补充。"
                        : "审批通过后会把这份 JD 备面建议确认为正式草案，方便继续在面试页补充。",
                writeObject(resolveJobPrepDraftPayload(snapshot), "{}"));
    }

    private RunBlueprint buildRecordingReviewBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        RecordingReviewSessionVO recordingReview = snapshot.recordingReview();
        StudyPlanPayload payload = resolveStudyPlanPayload(snapshot);
        String summary;
        List<String> recommendations;
        if (recordingReview != null) {
            String scoreText = formatNumber(recordingReview.getOverallScore());
            String weakPoint = firstItem(recordingReview.getWeakPoints());
            summary = "已把这次录音复盘的结论整理成训练动作。"
                    + (StringUtils.hasText(scoreText) ? " 当前复盘分 " + scoreText + "。" : "")
                    + (StringUtils.hasText(weakPoint) ? " 首要薄弱点是 " + weakPoint + "。" : "");
            recommendations = mergeRecommendations(
                    recordingReview.getSuggestedActions(),
                    nullSafeList(recordingReview.getWeakPoints()).isEmpty()
                            ? List.of()
                            : List.of("先回听薄弱片段，重点复盘 " + joinLimited(recordingReview.getWeakPoints(), 2, "、") + "。"),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入录音复盘后的训练排序。") : List.of(),
                    contextRefsText(contextRefs, "本次重点参考："));
        } else {
            summary = "已把录音复盘的结论整理成后续训练动作和复听重点。";
            recommendations = mergeRecommendations(
                    List.of("先回听薄弱片段，再改写成结构化口语答案。", "把复盘结果沉淀到下一轮模拟和错题复习里。"),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入录音复盘后的训练排序。") : List.of(),
                    contextRefsText(contextRefs, "本次重点参考："));
        }
        return blueprint(
                "录音复盘代理",
                summary,
                mergeRecommendations(
                        recommendations,
                        providerContextRecommendations("recording_review", snapshot)),
                List.of("查看转写片段", "提取薄弱点", "确认是否转成正式训练动作"),
                resolveRunNextActionPath(resolveRecordingReviewWorkspacePath(snapshot), "recording_review", snapshot),
                true,
                "save_recording_review_action",
                "审批通过后会把这次录音复盘结论保存为正式训练动作，并写入当前学习计划。",
                writeObject(resolveRecordingReviewActionPayload(snapshot, payload), "{}"));
    }

    private RunBlueprint buildResumeCoachBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        ResumeFileVO resume = snapshot.resume();
        JobApplicationVO application = snapshot.application();
        JobApplicationVO boardFocusApplication = snapshot.applicationBoard() == null ? null : snapshot.applicationBoard().focusApplication();
        String summary;
        List<String> baseRecommendations = new ArrayList<>();
        if (resume != null) {
            summary = "已围绕简历《" + defaultText(resume.getTitle(), "当前简历") + "》整理修改优先级和项目表达建议。";
            if ("failed".equalsIgnoreCase(resume.getParseStatus()) && StringUtils.hasText(resume.getParseError())) {
                baseRecommendations.add(resume.getParseError());
            }
            if (!StringUtils.hasText(resume.getSummary())) {
                baseRecommendations.add("先补 2-3 句简历摘要，明确方向、技术栈和项目亮点。");
            }
            if (!StringUtils.hasText(resume.getSelfIntro())) {
                baseRecommendations.add("补一版面试开场，让简历内容能自然过渡到项目表达。");
            }
            if (resume.getProjects() == null || resume.getProjects().isEmpty()) {
                baseRecommendations.add("至少补 1 个可展开讲职责、取舍和结果的项目经历。");
            } else {
                baseRecommendations.add("优先收紧最贴近目标岗位的项目顺序和量化结果表述。");
            }
            if (!nullSafeList(resume.getSkills()).isEmpty()) {
                baseRecommendations.add("保留最重要的技能关键词：" + joinLimited(resume.getSkills(), 4, "、") + "。");
            }
        } else {
            summary = "已按当前岗位目标整理简历修改与项目表达建议。";
            baseRecommendations.add("先收紧标题和摘要，再调整项目顺序。");
            baseRecommendations.add("优先补充最贴近岗位的量化结果与关键词。");
        }
        if (application != null && !nullSafeList(application.getMissingKeywords()).isEmpty()) {
            baseRecommendations.add("结合目标岗位，补齐这些关键词：" + joinLimited(application.getMissingKeywords(), 3, "、") + "。");
        } else if (boardFocusApplication != null && !nullSafeList(boardFocusApplication.getMissingKeywords()).isEmpty()) {
            summary = StringUtils.hasText(boardFocusApplication.getJobTitle())
                    ? "已按当前投递看板的焦点岗位「" + boardFocusApplication.getJobTitle() + "」整理简历修改与项目表达建议。"
                    : summary;
            baseRecommendations.add("结合当前优先投递岗位，补齐这些关键词："
                    + joinLimited(boardFocusApplication.getMissingKeywords(), 3, "、") + "。");
            if (StringUtils.hasText(boardFocusApplication.getReviewSuggestion())) {
                baseRecommendations.add(boardFocusApplication.getReviewSuggestion());
            }
        }
        List<String> recommendations = mergeRecommendations(
                baseRecommendations,
                providerContextRecommendations("resume_coach", snapshot),
                prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到简历修改排序。") : List.of(),
                contextRefsText(contextRefs, "当前基于以下材料："));
        return blueprint(
                "简历教练代理",
                summary,
                recommendations,
                List.of("确认目标岗位", "生成简历修改点", "决定是否写回简历版本"),
                resolveRunNextActionPath(resolveResumeWorkspacePath(snapshot), "resume_coach", snapshot),
                true,
                "save_resume_follow_up_draft",
                resume != null
                        ? "审批通过后会把简历《" + defaultText(resume.getTitle(), "当前简历") + "》的修改建议确认为正式草稿。"
                        : "审批通过后会把这次简历追问和修改建议确认为正式草稿。",
                writeObject(resolveResumeFollowUpDraftPayload(snapshot, summary, recommendations), "{}"));
    }

    private RunBlueprint buildApplicationStrategistBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        JobApplicationVO application = snapshot.application();
        ApplicationBoardSnapshot applicationBoard = snapshot.applicationBoard();
        JobApplicationVO focusApplication = application != null
                ? application
                : applicationBoard == null ? null : applicationBoard.focusApplication();
        String summary;
        List<String> recommendations;
        String nextActionPath = "/applications";
        if (application != null) {
            summary = "已根据 " + defaultText(application.getCompany(), "目标公司") + " " + defaultText(application.getJobTitle(), "目标岗位")
                    + " 的当前投递状态整理下一步推进建议。";
            recommendations = mergeRecommendations(
                    List.of("当前岗位状态：" + applicationStatusLabel(application.getStatus()) + "。"),
                    StringUtils.hasText(application.getNextStepSuggestion()) ? List.of(application.getNextStepSuggestion()) : List.of(),
                    StringUtils.hasText(application.getReviewSuggestion()) ? List.of(application.getReviewSuggestion()) : List.of(),
                    nullSafeList(application.getMissingKeywords()).isEmpty()
                            ? List.of("当前 JD 缺口不明显，可以把重点放到下一轮反馈和节奏推进。")
                            : List.of("优先补齐这些 JD 缺口：" + joinLimited(application.getMissingKeywords(), 3, "、") + "。"),
                    providerContextRecommendations("application_strategist", snapshot),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到投递推进排序。") : List.of(),
                    contextRefsText(contextRefs, "本次参考的岗位或反馈："));
            nextActionPath = "/applications/" + application.getId();
        } else if (focusApplication != null) {
            summary = "已根据当前投递看板整理下一步推进建议。当前共有 "
                    + defaultInt(applicationBoard.totalCount())
                    + " 条岗位记录，进行中 "
                    + defaultInt(applicationBoard.activeCount())
                    + " 条，优先推进 "
                    + defaultText(focusApplication.getCompany(), "当前重点公司")
                    + " "
                    + defaultText(focusApplication.getJobTitle(), "当前重点岗位")
                    + "。";
            recommendations = mergeRecommendations(
                    applicationBoardRecommendations(applicationBoard),
                    providerContextRecommendations("application_strategist", snapshot),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到投递推进排序。") : List.of(),
                    contextRefsText(contextRefs, "本次参考的岗位或反馈："));
            nextActionPath = "/applications/" + focusApplication.getId();
        } else {
            summary = "已根据当前投递状态和反馈节奏整理下一步推进建议。";
            recommendations = mergeRecommendations(
                    List.of("优先推进最接近面试的岗位，避免分散精力。", "把面试反馈标签同步到下一批岗位筛选标准。"),
                    providerContextRecommendations("application_strategist", snapshot),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到投递推进排序。") : List.of(),
                    contextRefsText(contextRefs, "本次参考的岗位或反馈："));
        }
        return blueprint(
                "投递策略代理",
                summary,
                recommendations,
                List.of("查看推进优先级", "确认下一步动作", "必要时进入待审批"),
                resolveRunNextActionPath(nextActionPath, "application_strategist", snapshot),
                true,
                "save_application_strategy",
                application != null
                        ? "审批通过后会把 " + defaultText(application.getJobTitle(), "当前岗位") + " 的投递推进建议确认为正式策略草案。"
                        : focusApplication != null
                                ? "审批通过后会把 " + defaultText(focusApplication.getJobTitle(), "当前焦点岗位") + " 的投递推进建议确认为正式策略草案。"
                                : "审批通过后会把这次投递推进建议确认为正式策略草案。",
                writeObject(new ApplicationStrategyPayload(
                        focusApplication == null ? null : focusApplication.getId(),
                        summary,
                        recommendations), "{}"));
    }

    private RunBlueprint buildInterviewReviewBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        InterviewDetailVO interviewDetail = snapshot.interviewDetail();
        CopilotRealtimeSessionVO copilotRealtimeSession = snapshot.copilotRealtimeSession();
        StudyPlanPayload payload = resolveStudyPlanPayload(snapshot);
        InterviewReviewActionPayload reviewActionPayload = resolveInterviewReviewActionPayload(snapshot, payload);
        String summary;
        List<String> baseRecommendations = new ArrayList<>();
        String nextActionPath = "/study-plan";
        if (interviewDetail != null) {
            int lowScoreCount = countLowScoreRecords(interviewDetail);
            summary = "已根据本轮 " + defaultText(interviewDetail.getDirection(), "模拟面试") + " 结果整理追问重点、低分点和下一轮训练建议。"
                    + (lowScoreCount > 0 ? " 当前共有 " + lowScoreCount + " 道低分题。" : "");
            baseRecommendations.addAll(interviewReviewRecommendations(interviewDetail));
            nextActionPath = "/interview/detail/" + interviewDetail.getSessionId();
        } else if (copilotRealtimeSession != null) {
            summary = defaultText(
                    copilotRealtimeSession.getPostInterviewReview() == null ? null : copilotRealtimeSession.getPostInterviewReview().getSummary(),
                    "已根据实时 Copilot 阶段整理面后复盘重点和下一轮训练建议。");
            baseRecommendations.addAll(copilotRealtimeRecommendations(copilotRealtimeSession));
            nextActionPath = resolveCopilotLiveWorkspacePath(snapshot);
        } else {
            summary = "已根据本轮面试结果整理追问重点、低分点和下一轮训练建议。";
            baseRecommendations.add("先处理低分题，再安排一轮同主题模拟。");
            baseRecommendations.add("把薄弱点转成错题或复习任务，避免只停留在摘要层。");
        }
        return blueprint(
                "面试复盘代理",
                summary,
                mergeRecommendations(
                        baseRecommendations,
                        providerContextRecommendations("interview_review", snapshot),
                        prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入下一轮面试复盘动作。") : List.of(),
                        contextRefsText(contextRefs, "复盘引用了这些上下文：")),
                List.of("汇总面试结果", "提取低分点", "决定是否刷新训练计划"),
                resolveRunNextActionPath(nextActionPath, "interview_review", snapshot),
                true,
                reviewActionPayload == null ? "refresh_study_plan" : "save_interview_review_action",
                reviewActionPayload == null
                        ? "审批通过后会刷新学习计划，把这次面试复盘结论转成正式训练动作。"
                        : "审批通过后会把这次面试复盘结论写成正式训练任务，方便在学习计划里直接执行。",
                reviewActionPayload == null ? writeObject(payload, "{}") : writeObject(reviewActionPayload, "{}"));
    }

    private RunBlueprint buildRealtimeCopilotBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        String summary;
        List<String> recommendations;
        String nextActionPath = resolveCopilotPrepWorkspacePath(snapshot);
        String nextActionAgentType = "realtime_copilot";
        CopilotPrepDraftPayload copilotPrepDraftPayload = resolveCopilotPrepDraftPayload(snapshot, prompt);
        boolean requiresApproval = false;
        String approvalActionType = null;
        String approvalSummary = null;
        String approvalPayloadJson = null;
        if (snapshot.copilotRealtimeSession() != null) {
            CopilotRealtimeSessionVO realtimeSession = snapshot.copilotRealtimeSession();
            summary = realtimeCopilotSummary(realtimeSession);
            nextActionPath = resolveCopilotLiveWorkspacePath(snapshot);
            recommendations = mergeRecommendations(
                    realtimeCopilotLiveRecommendations(realtimeSession),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到当前实时 Copilot 会话。") : List.of(),
                    contextRefsText(contextRefs, "实时阶段引用："));
            String reviewActionPath = realtimeReviewNextActionPath(realtimeSession);
            if (StringUtils.hasText(reviewActionPath)) {
                nextActionPath = reviewActionPath;
                nextActionAgentType = "interview_review";
            }
        } else if (snapshot.copilotPrepSession() != null) {
            CopilotPrepSessionVO prepSession = snapshot.copilotPrepSession();
            summary = "已根据 " + defaultText(prepSession.getCompany(), "当前岗位") + " "
                    + defaultText(prepSession.getJobTitle(), "会前 Prep")
                    + " 整理会前提示，后续可以直接进入实时阶段。";
            nextActionPath = resolveCopilotLiveWorkspacePath(snapshot);
            recommendations = mergeRecommendations(
                    List.of("先把 Copilot Prep 压成可口述的开场提纲，再建立实时连接。"),
                    limit(prepSession.getOpeningBrief(), 2),
                    limit(prepSession.getLiveCues(), 2),
                    limit(prepSession.getKeyRisks(), 1),
                    prepSession.getNextActions(),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”同步到实时 Copilot Prep。") : List.of(),
                    contextRefsText(contextRefs, "Prep 阶段引用："));
        } else if (snapshot.jobPrepSession() != null) {
            JobPrepSessionVO jobPrepSession = snapshot.jobPrepSession();
            summary = "已根据 " + defaultText(jobPrepSession.getCompany(), "当前岗位") + " " + defaultText(jobPrepSession.getJobTitle(), "会前准备")
                    + " 生成会前清单，后续可以继续接实时建议流。";
            nextActionPath = resolveCopilotPrepWorkspacePath(snapshot);
            recommendations = mergeRecommendations(
                    List.of("先把 JD 备面结果转成开场和追问清单。"),
                    jobPrepSession.getNextActions(),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入 Copilot Prep。") : List.of(),
                    contextRefsText(contextRefs, "Prep 阶段引用："));
            requiresApproval = copilotPrepDraftPayload != null;
        } else {
            summary = "已生成会前准备清单，后续可以继续接实时建议流。";
            recommendations = mergeRecommendations(
                    List.of("先完成 Copilot Prep，再进入实时阶段。", "把当前岗位、简历和 JD 统一成可速读的会前提纲。"),
                    prompt != null ? List.of("把用户补充目标“" + abbreviate(prompt, 20) + "”纳入 Copilot Prep。") : List.of(),
                    contextRefsText(contextRefs, "Prep 阶段引用："));
            requiresApproval = copilotPrepDraftPayload != null;
        }
        if (requiresApproval) {
            approvalActionType = "save_copilot_prep_draft";
            approvalSummary = "审批通过后会把这轮 Copilot Prep 建议写成正式会前草案，方便继续进入实时阶段。";
            approvalPayloadJson = writeObject(copilotPrepDraftPayload, "{}");
        }
        return blueprint(
                "实时 Copilot 代理",
                summary,
                mergeRecommendations(
                        recommendations,
                        providerContextRecommendations("realtime_copilot", snapshot)),
                List.of("会前准备", "连接实时会话", "面后复盘回写"),
                resolveRunNextActionPath(nextActionPath, nextActionAgentType, snapshot),
                requiresApproval,
                approvalActionType,
                approvalSummary,
                approvalPayloadJson);
    }

    private RunBlueprint buildCoordinatorBlueprint(List<String> contextRefs, String prompt, ContextSnapshot snapshot) {
        String nextActionPath = resolveCoordinatorNextActionPath(snapshot);
        String summary = "已把当前上下文收敛成统一的下一步动作清单。";
        List<String> recommendations = mergeRecommendations(
                coordinatorRecommendations(snapshot),
                providerContextRecommendations("coordinator", snapshot),
                prompt != null ? List.of("先围绕用户补充目标“" + abbreviate(prompt, 20) + "”安排优先级。") : List.of(),
                contextRefsText(contextRefs, "当前上下文："));
        return blueprint(
                "协调代理",
                summary,
                recommendations,
                List.of("识别任务类型", "路由到具体能力", "如涉及写操作则等待审批"),
                resolveRunNextActionPath(nextActionPath, "coordinator", snapshot),
                false,
                null,
                null,
                null);
    }

    private ExecutionResult executeApprovalAction(Long userId, AgentRun run) {
        String actionType = normalize(run.getApprovalActionType());
        return switch (actionType) {
            case "refresh_study_plan" -> executeStudyPlanAction(userId, run.getApprovalPayloadJson());
            case "save_interview_review_action" -> executeInterviewReviewAction(userId, run.getApprovalPayloadJson());
            case "save_topic_retrospective_action" -> executeTopicRetrospectiveAction(userId, run.getApprovalPayloadJson());
            case "save_job_prep_draft" -> executeJobPrepDraftAction(userId, run.getApprovalPayloadJson());
            case "save_copilot_prep_draft" -> executeCopilotPrepDraftAction(userId, run.getApprovalPayloadJson());
            case "save_resume_follow_up_draft" -> executeResumeFollowUpDraftAction(userId, run.getApprovalPayloadJson());
            case "save_application_strategy" -> executeApplicationStrategyAction(userId, run.getApprovalPayloadJson());
            case "save_recording_review_action" -> executeRecordingReviewAction(userId, run.getApprovalPayloadJson());
            default -> new ExecutionResult("已完成审批。", null, null);
        };
    }

    private ExecutionResult executeStudyPlanAction(Long userId, String payloadJson) {
        StudyPlanPayload payload = readObject(payloadJson, StudyPlanPayload.class);
        StudyPlanCurrentVO currentPlan = planService.current(userId);
        StudyPlanCurrentVO result;
        if (currentPlan == null) {
            StudyPlanGenerateRequest generateRequest = new StudyPlanGenerateRequest();
            if (payload != null && payload.durationDays() != null) {
                generateRequest.setDurationDays(payload.durationDays());
            }
            if (payload != null) {
                generateRequest.setFocusDirection(payload.focusDirection());
                generateRequest.setTargetRole(payload.targetRole());
                generateRequest.setTechStack(payload.techStack());
            }
            result = planService.generate(userId, generateRequest);
        } else {
            result = planService.refresh(userId, currentPlan.getId());
        }
        return new ExecutionResult(
                "已生成正式学习计划《" + result.getTitle() + "》，可以继续在学习计划页执行。",
                "前往学习计划",
                "/study-plan");
    }

    private ExecutionResult executeTopicRetrospectiveAction(Long userId, String payloadJson) {
        TopicRetrospectiveActionPayload payload = readObject(payloadJson, TopicRetrospectiveActionPayload.class);
        if (payload == null || payload.categoryId() == null) {
            return new ExecutionResult("当前缺少领域回顾对象，暂未保存正式训练动作。", null, null);
        }
        StudyPlanCurrentVO plan = planService.saveTopicRetrospectiveAction(
                userId,
                payload.categoryId(),
                payload.focusDirection(),
                payload.targetRole(),
                payload.techStack(),
                payload.taskTitle(),
                payload.taskDescription(),
                payload.actionPath());
        return new ExecutionResult(
                "已把这份领域回顾结论写成正式训练任务，可继续在学习计划页执行。",
                "查看训练任务",
                resolveSavedPlanTaskPath(plan, "topic_retrospective", payload.actionPath(), payload.taskTitle()));
    }

    private ExecutionResult executeJobPrepDraftAction(Long userId, String payloadJson) {
        JobPrepDraftPayload payload = readObject(payloadJson, JobPrepDraftPayload.class);
        if (payload == null || (!StringUtils.hasText(payload.jdText()) && payload.applicationId() == null)) {
            return new ExecutionResult("当前缺少可写入的 JD 备面内容，暂未生成正式草案。", null, null);
        }
        JobPrepSessionCreateRequest request = new JobPrepSessionCreateRequest();
        request.setApplicationId(payload.applicationId());
        request.setResumeId(payload.resumeId());
        request.setCompany(payload.company());
        request.setJobTitle(payload.jobTitle());
        request.setJdText(payload.jdText());
        JobPrepSessionVO session = interviewJobPrepService.createSession(userId, request);
        String title = firstNonBlank(session.getJobTitle(), payload.jobTitle(), "当前岗位");
        return new ExecutionResult(
                "已生成正式 JD 备面草案《" + title + "》，可以继续在面试页查看和消费。",
                "查看 JD 备面草案",
                "/interview?workspace=job-prep&jobPrepSessionId=" + session.getId());
    }

    private ExecutionResult executeCopilotPrepDraftAction(Long userId, String payloadJson) {
        CopilotPrepDraftPayload payload = readObject(payloadJson, CopilotPrepDraftPayload.class);
        if (payload == null || (!StringUtils.hasText(payload.jobTitle()) && payload.applicationId() == null
                && payload.jobPrepSessionId() == null && payload.resumeId() == null)) {
            return new ExecutionResult("当前缺少可写入的 Copilot Prep 内容，暂未生成正式草案。", null, null);
        }
        CopilotPrepSessionCreateRequest request = new CopilotPrepSessionCreateRequest();
        request.setApplicationId(payload.applicationId());
        request.setResumeId(payload.resumeId());
        request.setJobPrepSessionId(payload.jobPrepSessionId());
        request.setCompany(payload.company());
        request.setJobTitle(payload.jobTitle());
        request.setJdText(payload.jdText());
        request.setNotes(payload.notes());
        CopilotPrepSessionVO session = interviewCopilotPrepService.createSession(userId, request);
        String title = firstNonBlank(session.getJobTitle(), payload.jobTitle(), "当前岗位");
        return new ExecutionResult(
                "已生成正式 Copilot Prep 草案《" + title + "》，可以继续在面试页进入实时阶段。",
                "查看 Copilot Prep",
                "/interview?workspace=copilot-prep&copilotPrepSessionId=" + session.getId());
    }

    private ExecutionResult executeApplicationStrategyAction(Long userId, String payloadJson) {
        ApplicationStrategyPayload payload = readObject(payloadJson, ApplicationStrategyPayload.class);
        if (payload == null || payload.applicationId() == null) {
            return new ExecutionResult("当前缺少可写入的投递对象，暂未保存正式策略草案。", null, null);
        }
        JobApplicationVO application = jobApplicationService.saveStrategyDraft(
                userId, payload.applicationId(), payload.summary(), payload.recommendations());
        return new ExecutionResult("已把投递策略草案写回「"
                + defaultText(application.getCompany(), "目标公司")
                + " / "
                + defaultText(application.getJobTitle(), "目标岗位")
                + "」，可继续在投递页执行。",
                "查看投递详情",
                "/applications/" + application.getId());
    }

    private ExecutionResult executeResumeFollowUpDraftAction(Long userId, String payloadJson) {
        ResumeFollowUpDraftPayload payload = readObject(payloadJson, ResumeFollowUpDraftPayload.class);
        if (payload == null || payload.resumeId() == null) {
            return new ExecutionResult("当前缺少可写入的简历对象，暂未保存正式追问草稿。", null, null);
        }
        ResumeFileVO resume = resumeService.saveFollowUpDraft(
                userId, payload.resumeId(), payload.summary(), payload.recommendations());
        return new ExecutionResult("已把简历追问草稿写回「"
                + defaultText(resume.getTitle(), "当前简历")
                + "」，可继续在简历页整理和消费。",
                "查看简历草稿",
                "/resume?resumeId=" + resume.getId());
    }

    private ExecutionResult executeRecordingReviewAction(Long userId, String payloadJson) {
        RecordingReviewActionPayload payload = readObject(payloadJson, RecordingReviewActionPayload.class);
        if (payload == null || payload.recordingReviewSessionId() == null) {
            return new ExecutionResult("当前缺少录音复盘对象，暂未保存正式训练动作。", null, null);
        }
        StudyPlanCurrentVO plan = planService.saveRecordingReviewAction(
                userId,
                payload.recordingReviewSessionId(),
                payload.focusDirection(),
                payload.targetRole(),
                payload.techStack(),
                payload.taskTitle(),
                payload.taskDescription(),
                payload.actionPath());
        return new ExecutionResult(
                "已把这次录音复盘结论写成正式训练任务，可继续在学习计划页执行。",
                "查看训练任务",
                resolveSavedPlanTaskPath(plan, "recording_review", payload.actionPath(), payload.taskTitle()));
    }

    private ExecutionResult executeInterviewReviewAction(Long userId, String payloadJson) {
        InterviewReviewActionPayload payload = readObject(payloadJson, InterviewReviewActionPayload.class);
        if (payload == null || (payload.interviewSessionId() == null && payload.copilotRealtimeSessionId() == null)) {
            return new ExecutionResult("当前缺少面试复盘对象，暂未保存正式训练动作。", null, null);
        }
        StudyPlanCurrentVO plan = planService.saveInterviewReviewAction(
                userId,
                payload.interviewSessionId(),
                payload.copilotRealtimeSessionId(),
                payload.focusDirection(),
                payload.targetRole(),
                payload.techStack(),
                payload.taskTitle(),
                payload.taskDescription(),
                payload.actionPath());
        return new ExecutionResult(
                "已把这次面试复盘结论写成正式训练任务，可继续在学习计划页执行。",
                "查看训练任务",
                resolveSavedPlanTaskPath(plan, "interview_review", payload.actionPath(), payload.taskTitle()));
    }

    private ContextSnapshot resolveContextSnapshot(Long userId, List<String> contextRefs) {
        AbilityProfileVO abilityProfile = null;
        ProfileTopicDetailVO topicDetail = null;
        ProfileTopicRetrospectiveVO topicRetrospective = null;
        WeakTopicSnapshot weakTopicSnapshot = null;
        DashboardOverviewVO dashboardOverview = null;
        StudyPlanCurrentVO currentPlan = null;
        InterviewDetailVO interviewDetail = null;
        RecordingReviewSessionVO recordingReview = null;
        KnowledgeDocVO knowledgeDoc = null;
        QuestionVO question = null;
        WrongQuestionVO wrongQuestion = null;
        ResumeFileVO resume = null;
        JobApplicationVO application = null;
        ApplicationBoardSnapshot applicationBoard = null;
        JobPrepSessionVO jobPrepSession = null;
        CopilotPrepSessionVO copilotPrepSession = null;
        CopilotRealtimeSessionVO copilotRealtimeSession = null;
        List<UserProviderConfigItemVO> providerConfigs = null;

        if (hasContext(contextRefs, "dashboard:overview")) {
            dashboardOverview = loadOptional("dashboard overview", dashboardService::overview);
        }

        String topicNameRef = findContextRefValue(contextRefs, "analytics:topic-name:");

        if (hasContext(contextRefs, "analytics:profile")
                || hasContext(contextRefs, "analytics:weak-topics")
                || StringUtils.hasText(topicNameRef)) {
            abilityProfile = loadOptional("analytics profile", () -> analyticsService.getAbilityProfile(userId));
            if (abilityProfile != null && hasContext(contextRefs, "analytics:weak-topics")) {
                weakTopicSnapshot = buildWeakTopicSnapshot(abilityProfile);
            }
        }

        if (hasContext(contextRefs, "study-plan:active")) {
            currentPlan = loadOptional("active study plan", () -> planService.current(userId));
        }

        Long topicId = findContextRefId(contextRefs, "analytics:topic:");
        if (topicId != null) {
            topicDetail = loadOptional("analytics topic " + topicId, () -> analyticsService.getProfileTopicDetail(userId, topicId));
        } else if (StringUtils.hasText(topicNameRef) && abilityProfile != null) {
            Long resolvedTopicId = resolveTopicIdByName(abilityProfile, topicNameRef);
            if (resolvedTopicId != null) {
                Long targetTopicId = resolvedTopicId;
                topicDetail = loadOptional(
                        "analytics topic " + targetTopicId,
                        () -> analyticsService.getProfileTopicDetail(userId, targetTopicId));
            }
        }

        Long retrospectiveTopicId = findContextRefId(contextRefs, "analytics:retrospective:topic:");
        if (retrospectiveTopicId != null) {
            Long targetTopicId = retrospectiveTopicId;
            topicRetrospective = loadOptional(
                    "analytics retrospective " + retrospectiveTopicId,
                    () -> analyticsService.buildProfileTopicRetrospective(userId, targetTopicId));
            if (topicDetail == null) {
                topicDetail = loadOptional(
                        "analytics topic " + retrospectiveTopicId,
                        () -> analyticsService.getProfileTopicDetail(userId, targetTopicId));
            }
        }

        Long interviewSessionId = findContextRefId(contextRefs, "interview:session:");
        if (interviewSessionId != null) {
            interviewDetail = loadOptional("interview session " + interviewSessionId, () -> interviewService.detail(userId, interviewSessionId));
        } else if (hasContext(contextRefs, "interview:latest")) {
            InterviewHistoryVO latest = loadOptional("latest interview", () -> {
                List<InterviewHistoryVO> history = interviewService.trendData(userId, 1);
                return history == null || history.isEmpty() ? null : history.get(0);
            });
            if (latest != null && latest.getSessionId() != null) {
                Long latestSessionId = latest.getSessionId();
                interviewDetail = loadOptional("interview session " + latestSessionId, () -> interviewService.detail(userId, latestSessionId));
            }
        }

        Long recordingReviewId = findContextRefId(contextRefs, "interview:recording-review:");
        if (recordingReviewId != null) {
            recordingReview = loadOptional(
                    "recording review " + recordingReviewId,
                    () -> interviewRecordingReviewService.detail(userId, recordingReviewId));
        } else if (hasContext(contextRefs, "interview:recording-review")) {
            recordingReview = loadOptional("latest recording review", () -> interviewRecordingReviewService.latest(userId));
        }

        Long knowledgeDocId = findContextRefId(contextRefs, "knowledge:");
        if (knowledgeDocId != null) {
            knowledgeDoc = loadOptional("knowledge doc " + knowledgeDocId, () -> knowledgeService.detailDoc(userId, knowledgeDocId));
        }

        Long questionId = findContextRefId(contextRefs, "question:");
        if (questionId != null) {
            question = loadOptional("question " + questionId, () -> questionService.getQuestionDetail(questionId));
        }

        Long wrongQuestionId = findContextRefId(contextRefs, "wrong:");
        if (wrongQuestionId != null) {
            wrongQuestion = loadOptional("wrong question " + wrongQuestionId, () -> wrongService.detail(userId, wrongQuestionId));
        }

        Long jobPrepSessionId = findContextRefId(contextRefs, "interview:job-prep:");
        if (jobPrepSessionId != null) {
            jobPrepSession = loadOptional("job prep " + jobPrepSessionId, () -> interviewJobPrepService.detail(userId, jobPrepSessionId));
        } else if (hasContext(contextRefs, "interview:job-prep")) {
            jobPrepSession = loadOptional("latest job prep", () -> interviewJobPrepService.latest(userId));
        }

        Long copilotPrepSessionId = findContextRefId(contextRefs, "interview:copilot-prep:");
        if (copilotPrepSessionId != null) {
            copilotPrepSession = loadOptional(
                    "copilot prep " + copilotPrepSessionId,
                    () -> interviewCopilotPrepService.detail(userId, copilotPrepSessionId));
        } else if (hasContext(contextRefs, "interview:copilot-prep")) {
            copilotPrepSession = loadOptional("latest copilot prep", () -> interviewCopilotPrepService.latest(userId));
        }

        Long copilotRealtimeSessionId = findContextRefId(contextRefs, "interview:copilot-realtime:");
        if (copilotRealtimeSessionId != null) {
            copilotRealtimeSession = loadOptional(
                    "copilot realtime " + copilotRealtimeSessionId,
                    () -> interviewCopilotRealtimeService.detail(userId, copilotRealtimeSessionId));
        } else if (hasContext(contextRefs, "interview:copilot-realtime")) {
            copilotRealtimeSession = loadOptional("latest copilot realtime", () -> interviewCopilotRealtimeService.latest(userId));
        }

        Long resumeId = findContextRefId(contextRefs, "resume:");
        if (resumeId != null) {
            resume = loadOptional("resume " + resumeId, () -> resumeService.detail(userId, resumeId));
        } else if (hasContext(contextRefs, "resume:latest")) {
            resume = loadOptional("latest resume", () -> resumeService.latest(userId));
        }

        Long applicationId = findContextRefId(contextRefs, "application:");
        if (applicationId != null) {
            application = loadOptional("application " + applicationId, () -> jobApplicationService.detail(userId, applicationId));
        } else if (hasContext(contextRefs, "application:board")) {
            applicationBoard = loadOptional("application board", () -> {
                List<JobApplicationVO> board = jobApplicationService.board(userId);
                return buildApplicationBoardSnapshot(board);
            });
        }

        providerConfigs = loadProviderConfigs();

        return new ContextSnapshot(
                abilityProfile,
                topicDetail,
                topicRetrospective,
                weakTopicSnapshot,
                dashboardOverview,
                currentPlan,
                interviewDetail,
                recordingReview,
                knowledgeDoc,
                question,
                wrongQuestion,
                resume,
                application,
                applicationBoard,
                jobPrepSession,
                copilotPrepSession,
                copilotRealtimeSession,
                providerConfigs);
    }

    private List<String> studyPlannerRecommendations(ContextSnapshot snapshot) {
        List<String> recommendations = new ArrayList<>();
        if (snapshot.topicDetail() != null) {
            ProfileTopicDetailVO topicDetail = snapshot.topicDetail();
            recommendations.add("当前重点领域是 " + topicDetail.getCategoryName()
                    + "，画像分 " + formatNumber(topicDetail.getAbilityScore())
                    + "，待复盘 " + defaultInt(topicDetail.getDueCount()) + " 项。");
            recommendations.addAll(limit(topicDetail.getFocusRecommendations(), 2));
            if (snapshot.topicRetrospective() != null) {
                recommendations.addAll(retrospectiveRecommendations(snapshot.topicRetrospective()));
            }
        } else if (snapshot.weakTopicSnapshot() != null) {
            WeakTopicSnapshot weakTopicSnapshot = snapshot.weakTopicSnapshot();
            recommendations.add("当前弱项主题优先级是 "
                    + defaultText(joinLimited(weakTopicSnapshot.topicLabels(), 3, "、"), "当前薄弱点")
                    + "。");
            if (StringUtils.hasText(weakTopicSnapshot.focusTopicName())) {
                recommendations.add("下一轮计划先收紧「" + weakTopicSnapshot.focusTopicName()
                        + "」，再扩展到相邻薄弱主题。");
            }
        } else if (snapshot.abilityProfile() != null) {
            AbilityProfileVO profile = snapshot.abilityProfile();
            recommendations.add("长期画像建议优先处理 "
                    + defaultText(profile.getSuggestedFocus(), "当前薄弱点")
                    + "，推荐强度 " + difficultyLabel(profile.getRecommendedDifficulty()) + "。");
        }
        if (snapshot.currentPlan() != null) {
            StudyPlanCurrentVO currentPlan = snapshot.currentPlan();
            recommendations.add("当前正式计划是《" + defaultText(currentPlan.getTitle(), "本轮学习计划")
                    + "》，当前 Day " + defaultInt(currentPlan.getCurrentDay())
                    + " / " + defaultInt(currentPlan.getDurationDays())
                    + "，今日还有 " + defaultInt(currentPlan.getTodayTaskCount()) + " 项任务。");
            if (currentPlan.getTodayFocusSummary() != null && StringUtils.hasText(currentPlan.getTodayFocusSummary().getReason())) {
                recommendations.add("今天先推进：" + abbreviate(currentPlan.getTodayFocusSummary().getReason(), 40));
            }
            firstPendingPlanTask(currentPlan).ifPresent(task -> recommendations.add(
                    "如果要刷新计划，先确认任务「" + defaultText(task.getTitle(), defaultText(task.getModule(), "当前任务"))
                            + "」是否仍应保留在今天。"));
        }
        if (snapshot.interviewDetail() != null) {
            int lowScoreCount = countLowScoreRecords(snapshot.interviewDetail());
            if (lowScoreCount > 0) {
                recommendations.add("最近模拟面试有 " + lowScoreCount + " 道低分题，先把低分题改写成结构化答案。");
            }
        }
        if (snapshot.knowledgeDoc() != null) {
            KnowledgeDocVO knowledgeDoc = snapshot.knowledgeDoc();
            recommendations.add("当前资料焦点是《" + defaultText(knowledgeDoc.getTitle(), "当前资料")
                    + "》，适合先把它拆成可练习的问题或表达提纲。");
            if (StringUtils.hasText(knowledgeDoc.getSummary())) {
                recommendations.add("先消化这份资料的核心内容：" + abbreviate(knowledgeDoc.getSummary(), 42));
            }
            if ("jd".equalsIgnoreCase(knowledgeDoc.getBusinessType())) {
                recommendations.add("这份资料属于 JD 方向，下一步优先转成备面问题和项目追问清单。");
            }
        }
        if (snapshot.question() != null) {
            QuestionVO question = snapshot.question();
            recommendations.add("当前题库焦点题是「" + defaultText(question.getTitle(), "当前题目")
                    + "」，适合把它改写成一段完整口语答案。");
            if (StringUtils.hasText(question.getFollowUpSuggestions())) {
                recommendations.add("这道题的训练提醒：" + abbreviate(question.getFollowUpSuggestions(), 40));
            }
            if (StringUtils.hasText(question.getCommonMistakes())) {
                recommendations.add("回答时先避开这个常见问题：" + abbreviate(question.getCommonMistakes(), 36));
            }
        }
        if (snapshot.wrongQuestion() != null) {
            WrongQuestionVO wrongQuestion = snapshot.wrongQuestion();
            recommendations.add("当前错题「" + defaultText(wrongQuestion.getTitle(), "这道题")
                    + "」还没有彻底稳定，建议优先安排一次针对性复述。");
            if (StringUtils.hasText(wrongQuestion.getErrorReason())) {
                recommendations.add("先处理这道错题的卡点：" + abbreviate(wrongQuestion.getErrorReason(), 40));
            }
        }
        if (snapshot.recordingReview() != null && !nullSafeList(snapshot.recordingReview().getWeakPoints()).isEmpty()) {
            recommendations.add("录音复盘暴露的首要薄弱点是 "
                    + firstItem(snapshot.recordingReview().getWeakPoints())
                    + "，先安排专项口语复盘。");
        }
        if (snapshot.application() != null && !nullSafeList(snapshot.application().getMissingKeywords()).isEmpty()) {
            recommendations.add("目标岗位仍缺 "
                    + joinLimited(snapshot.application().getMissingKeywords(), 2, "、")
                    + " 关键词，计划里要补这组内容。");
        } else if (snapshot.applicationBoard() != null
                && snapshot.applicationBoard().focusApplication() != null
                && !nullSafeList(snapshot.applicationBoard().focusApplication().getMissingKeywords()).isEmpty()) {
            JobApplicationVO focusApplication = snapshot.applicationBoard().focusApplication();
            recommendations.add("当前优先投递岗位「"
                    + defaultText(focusApplication.getJobTitle(), defaultText(focusApplication.getCompany(), "当前焦点岗位"))
                    + "」仍缺 "
                    + joinLimited(focusApplication.getMissingKeywords(), 2, "、")
                    + " 关键词，下一轮计划要补这组内容。");
            if (StringUtils.hasText(focusApplication.getNextStepSuggestion())) {
                recommendations.add("看板推进提醒：" + abbreviate(focusApplication.getNextStepSuggestion(), 40));
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add("先处理到期待复盘，再安排新的专项训练。");
            recommendations.add("把低分点拆成 2-3 个可执行任务，避免计划过长。");
        }
        return recommendations;
    }

    private String realtimeCopilotSummary(CopilotRealtimeSessionVO session) {
        String label = defaultText(session.getCompany(), "当前岗位") + " / " + defaultText(session.getJobTitle(), "实时阶段");
        return switch (normalize(session.getStatus())) {
            case "live" -> label + " 的实时 Copilot 已连接，可以继续按当前会话信号调整回答。";
            case "awaiting_connection" -> label + " 的实时 Copilot 会话已创建，等待建立连接。";
            case "disconnected" -> label + " 的实时 Copilot 已断开，适合先决定重连还是转入面后复盘。";
            case "completed" -> defaultText(
                    session.getPostInterviewReview() == null ? null : session.getPostInterviewReview().getSummary(),
                    label + " 的实时阶段已结束，下一步适合整理面后复盘。");
            default -> label + " 的实时 Copilot 会话已就绪。";
        };
    }

    private List<String> realtimeCopilotLiveRecommendations(CopilotRealtimeSessionVO session) {
        List<String> recommendations = new ArrayList<>();
        if (StringUtils.hasText(session.getLatestEventSummary())) {
            recommendations.add("当前会话最新状态：" + abbreviate(session.getLatestEventSummary(), 48));
        }
        if (!nullSafeList(session.getLiveChecklist()).isEmpty()) {
            recommendations.add("优先盯住这些实时检查清单：" + joinLimited(session.getLiveChecklist(), 2, "；") + "。");
        }
        switch (normalize(session.getStatus())) {
            case "live" -> recommendations.add("当前实时连接已建立，建议边答边收束重点，避免现场追问失焦。");
            case "awaiting_connection" -> recommendations.add("先确认 WebSocket 连接和会前提纲都已就绪，再正式进入实时阶段。");
            case "disconnected" -> recommendations.add("如果面试还在继续，先尝试恢复连接；否则立即整理现场备注，避免细节丢失。");
            case "completed" -> recommendations.addAll(copilotRealtimeRecommendations(session));
            default -> {
            }
        }
        if ("degraded".equals(normalize(session.getProviderStatus()))) {
            recommendations.add("当前实时阶段处于降级模式，建议优先保留关键追问和现场备注，避免过度依赖自动能力。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("先确认当前实时阶段是要继续连接、补记录，还是直接转入面后复盘。");
        }
        return recommendations;
    }

    private List<String> retrospectiveRecommendations(ProfileTopicRetrospectiveVO retrospective) {
        List<String> recommendations = new ArrayList<>();
        if (!nullSafeList(retrospective.getRiskSignals()).isEmpty()) {
            recommendations.add("领域回顾提示当前风险：" + joinLimited(retrospective.getRiskSignals(), 2, "；") + "。");
        }
        recommendations.addAll(limit(retrospective.getNextActions(), 2));
        return recommendations;
    }

    private List<String> interviewReviewRecommendations(InterviewDetailVO interviewDetail) {
        List<String> recommendations = new ArrayList<>();
        List<String> weakTags = collectWeakPointTags(interviewDetail);
        if (!weakTags.isEmpty()) {
            recommendations.add("优先补这些薄弱点：" + joinLimited(weakTags, 3, "、") + "。");
        }
        nullSafeList(interviewDetail.getRecords()).stream()
                .filter(record -> Boolean.TRUE.equals(record.getIsLowScore()))
                .limit(2)
                .forEach(record -> {
                    if (StringUtils.hasText(record.getReviewSummary())) {
                        recommendations.add(record.getQuestionTitle() + "："
                                + abbreviate(record.getReviewSummary(), 40));
                    } else if (StringUtils.hasText(record.getComment())) {
                        recommendations.add(record.getQuestionTitle() + "："
                                + abbreviate(record.getComment(), 40));
                    }
                });
        if (recommendations.isEmpty()) {
            recommendations.add("先处理低分题，再安排一轮同主题模拟。");
            recommendations.add("把薄弱点转成错题或复习任务，避免只停留在摘要层。");
        }
        return recommendations;
    }

    private List<String> copilotRealtimeRecommendations(CopilotRealtimeSessionVO session) {
        List<String> recommendations = new ArrayList<>();
        CopilotRealtimeSessionVO.PostInterviewReviewVO review = session.getPostInterviewReview();
        if (review != null) {
            recommendations.addAll(limit(review.getRecommendedActions(), 3));
            if (!nullSafeList(review.getWeakPoints()).isEmpty()) {
                recommendations.add("优先处理这些实时阶段暴露的问题：" + joinLimited(review.getWeakPoints(), 2, "；") + "。");
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add("先补一轮面后复盘，把现场追问、卡壳点和表达缺口写清楚。");
        }
        if (session.getEndedAt() != null) {
            recommendations.add("实时阶段已经结束，适合立即把结论写回下一轮训练计划。");
        }
        return recommendations;
    }

    private String realtimeReviewNextActionPath(CopilotRealtimeSessionVO session) {
        if (session == null || session.getPostInterviewReview() == null) {
            return null;
        }
        return trimToNull(session.getPostInterviewReview().getNextActionPath());
    }

    private List<String> coordinatorRecommendations(ContextSnapshot snapshot) {
        List<String> recommendations = new ArrayList<>();
        if (snapshot.dashboardOverview() != null) {
            DashboardOverviewVO dashboardOverview = snapshot.dashboardOverview();
            NextActionVO nextAction = dashboardOverview.getNextAction();
            if (nextAction != null && StringUtils.hasText(nextAction.getTitle())) {
                recommendations.add("工作台当前主动作是「" + nextAction.getTitle() + "」，优先级 "
                        + defaultText(nextAction.getPriority(), "P1") + "。");
                if (StringUtils.hasText(nextAction.getReason())) {
                    recommendations.add("这一步优先的原因是：" + abbreviate(nextAction.getReason(), 40));
                }
            }
            if (defaultInt(dashboardOverview.getReviewDebtCount()) > 0) {
                recommendations.add("工作台里还有 " + defaultInt(dashboardOverview.getReviewDebtCount())
                        + " 项待巩固内容，今天要给复习债务留出处理时间。");
            }
            if (dashboardOverview.getApplicationSummary() != null
                    && defaultInt(dashboardOverview.getApplicationSummary().getActiveCount()) > 0) {
                recommendations.add("当前还有 "
                        + defaultInt(dashboardOverview.getApplicationSummary().getActiveCount())
                        + " 条进行中投递，注意把训练节奏和投递推进对齐。");
            }
            if (!nullSafeList(dashboardOverview.getWeakPoints()).isEmpty()) {
                recommendations.add("工作台薄弱点首先暴露在 "
                        + defaultText(dashboardOverview.getWeakPoints().get(0).getCategoryName(), "当前薄弱领域")
                        + "，适合优先转成训练动作。");
            }
        }
        if (snapshot.application() != null) {
            recommendations.add("先推进 " + defaultText(snapshot.application().getCompany(), "当前岗位")
                    + " 的投递动作，再决定是否扩展到下一批岗位。");
        } else if (snapshot.applicationBoard() != null && snapshot.applicationBoard().focusApplication() != null) {
            JobApplicationVO focusApplication = snapshot.applicationBoard().focusApplication();
            recommendations.add("投递看板当前最值得推进的是 "
                    + defaultText(focusApplication.getCompany(), "当前重点公司")
                    + " "
                    + defaultText(focusApplication.getJobTitle(), "当前重点岗位")
                    + "，先围绕这条线推进。");
        }
        if (snapshot.interviewDetail() != null) {
            recommendations.add("最近模拟面试已经沉淀结果，先完成复盘再决定下一轮训练。");
        }
        if (snapshot.recordingReview() != null) {
            recommendations.add("真实录音已经形成复盘证据，优先把薄弱点转成正式训练动作。");
        }
        if (snapshot.knowledgeDoc() != null) {
            KnowledgeDocVO knowledgeDoc = snapshot.knowledgeDoc();
            if ("jd".equalsIgnoreCase(knowledgeDoc.getBusinessType())) {
                recommendations.add("这份知识资料已经是岗位背景材料，优先把它接到 JD 备面和会前清单里。");
            } else if ("resume".equalsIgnoreCase(knowledgeDoc.getBusinessType())) {
                recommendations.add("这份资料和简历表达直接相关，适合先收紧项目表述再进入模拟面试。");
            } else {
                recommendations.add("这份资料已经能提供训练素材，下一步可以转成专项训练或计划任务。");
            }
        }
        if (snapshot.question() != null) {
            QuestionVO question = snapshot.question();
            recommendations.add("题库里的「" + defaultText(question.getTitle(), "当前题目")
                    + "」已经指向具体训练对象，适合直接转成专项练习或表达检验。");
        }
        if (snapshot.wrongQuestion() != null) {
            WrongQuestionVO wrongQuestion = snapshot.wrongQuestion();
            recommendations.add("错题「" + defaultText(wrongQuestion.getTitle(), "当前错题")
                    + "」已经暴露复习缺口，优先把它接回下一轮训练计划。");
        }
        if (snapshot.copilotRealtimeSession() != null) {
            recommendations.add(realtimeCoordinatorHint(snapshot.copilotRealtimeSession()));
        }
        if (snapshot.copilotPrepSession() != null) {
            recommendations.add("Copilot Prep 已整理完成，下一步可以直接进入实时阶段或补一轮会前口语演练。");
        }
        if (snapshot.topicRetrospective() != null) {
            recommendations.add("领域回顾已经生成，可以直接把阶段性风险和下一步动作转成正式计划。");
        }
        if (snapshot.resume() != null) {
            recommendations.add("简历材料已就绪，接下来优先处理项目表达和岗位关键词对齐。");
        }
        if (snapshot.abilityProfile() != null) {
            recommendations.add("长期画像建议先补 " + defaultText(snapshot.abilityProfile().getSuggestedFocus(), "当前薄弱点") + "。");
        }
        if (snapshot.weakTopicSnapshot() != null && StringUtils.hasText(snapshot.weakTopicSnapshot().focusTopicName())) {
            recommendations.add("当前最该收紧的弱项主题是 "
                    + snapshot.weakTopicSnapshot().focusTopicName()
                    + "，适合优先转成专项训练。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("先确认你现在要推进的是训练、备面、简历还是投递。");
            recommendations.add("把结果写入对应模块，而不是停留在对话摘要。");
        }
        return recommendations;
    }

    private String realtimeCoordinatorHint(CopilotRealtimeSessionVO session) {
        return switch (normalize(session.getStatus())) {
            case "live" -> "实时 Copilot 当前仍在连接中，先围绕现场信号和检查清单继续推进。";
            case "awaiting_connection" -> "实时 Copilot 会话已创建，下一步先完成连接并进入实时阶段。";
            case "disconnected" -> "实时 Copilot 已断开，先决定是否重连，或立即整理现场备注转入复盘。";
            case "completed" -> "实时 Copilot 已结束，先把现场备注和追问链路整理成正式复盘。";
            default -> "实时 Copilot 会话已就绪，建议先确认当前阶段应该继续连接还是转入复盘。";
        };
    }

    private List<String> applicationBoardRecommendations(ApplicationBoardSnapshot boardSnapshot) {
        if (boardSnapshot == null || boardSnapshot.focusApplication() == null) {
            return List.of();
        }
        JobApplicationVO focusApplication = boardSnapshot.focusApplication();
        List<String> recommendations = new ArrayList<>();
        recommendations.add("当前看板里进行中岗位有 " + defaultInt(boardSnapshot.activeCount())
                + " 条，先聚焦最接近推进节点的岗位。");
        recommendations.add("当前首要推进岗位状态："
                + applicationStatusLabel(focusApplication.getStatus())
                + "。");
        if (StringUtils.hasText(focusApplication.getNextStepSuggestion())) {
            recommendations.add(focusApplication.getNextStepSuggestion());
        }
        if (StringUtils.hasText(focusApplication.getReviewSuggestion())) {
            recommendations.add(focusApplication.getReviewSuggestion());
        }
        if (!nullSafeList(focusApplication.getMissingKeywords()).isEmpty()) {
            recommendations.add("优先补齐这条重点岗位的 JD 缺口："
                    + joinLimited(focusApplication.getMissingKeywords(), 3, "、") + "。");
        }
        return recommendations;
    }

    private WeakTopicSnapshot buildWeakTopicSnapshot(AbilityProfileVO profile) {
        if (profile == null) {
            return null;
        }
        List<String> weakCategories = nullSafeList(profile.getWeakCategories()).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        List<CategoryAbilityVO> sortedWeakTopics = nullSafeList(profile.getCategoryAbilities()).stream()
                .filter(item -> item != null && StringUtils.hasText(item.getCategoryName()))
                .filter(item -> Boolean.TRUE.equals(item.getIsWeak()) || weakCategories.contains(item.getCategoryName()))
                .sorted((left, right) -> {
                    double leftScore = left.getAbilityScore() == null ? Double.MAX_VALUE : left.getAbilityScore();
                    double rightScore = right.getAbilityScore() == null ? Double.MAX_VALUE : right.getAbilityScore();
                    int scoreCompare = Double.compare(leftScore, rightScore);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return defaultText(left.getCategoryName(), "").compareTo(defaultText(right.getCategoryName(), ""));
                })
                .limit(3)
                .toList();
        if (sortedWeakTopics.isEmpty()) {
            return null;
        }
        CategoryAbilityVO focusTopic = sortedWeakTopics.get(0);
        List<String> topicLabels = sortedWeakTopics.stream()
                .map(item -> item.getCategoryName() + "（" + defaultText(formatNumber(item.getAbilityScore()), "-") + "）")
                .toList();
        return new WeakTopicSnapshot(
                sortedWeakTopics,
                focusTopic.getCategoryId(),
                focusTopic.getCategoryName(),
                focusTopic.getRecommendedDifficulty(),
                topicLabels);
    }

    private List<String> providerContextRecommendations(String agentType, ContextSnapshot snapshot) {
        if (snapshot.providerConfigs() == null) {
            return List.of();
        }
        List<AgentRunVO.ProviderGateVO> providerGates = resolveProviderGates(agentType, snapshot.providerConfigs(), snapshot);
        String overallStatus = resolveProviderGateStatus(providerGates);
        boolean recordingReviewAgent = "recording_review".equals(normalize(agentType));
        boolean asrUnavailable = providerGates.stream()
                .anyMatch(item -> "asr".equals(normalize(item.getScope())) && !isProviderAvailable(item.getStatus()));
        boolean ossUnavailable = providerGates.stream()
                .anyMatch(item -> "oss".equals(normalize(item.getScope())) && !isProviderAvailable(item.getStatus()));
        if ("blocked".equals(overallStatus)) {
            return List.of(
                    "先去设置页补齐 "
                            + defaultText(joinLimited(unavailableProviderLabels(providerGates, true), 3, "、"), "关键 provider")
                            + " 配置，否则当前关键动作会被阻断。",
                    "补齐配置后再重跑这轮 agent，可以拿到完整的分析和下一步动作。");
        }
        if (recordingReviewAgent && asrUnavailable) {
            List<String> recommendations = new ArrayList<>();
            recommendations.add("当前语音识别未就绪，录音上传会被禁用，但可以先改用文字 transcript 模式继续录音复盘。");
            if (ossUnavailable) {
                recommendations.add("对象存储也还没完全就绪，长音频上传和回放承载能力会进一步降级。");
            }
            return recommendations;
        }
        if ("degraded".equals(overallStatus)) {
            return List.of("当前仍有 provider 未完全就绪："
                    + defaultText(joinLimited(unavailableProviderLabels(providerGates, false), 3, "、"), "部分依赖")
                    + "，相关结果会降级。");
        }
        return List.of();
    }

    private StudyPlanPayload resolveStudyPlanPayload(ContextSnapshot snapshot) {
        String focusDirection = firstNonBlank(
                snapshot.topicDetail() == null ? null : snapshot.topicDetail().getCategoryName(),
                snapshot.topicRetrospective() == null ? null : snapshot.topicRetrospective().getCategoryName(),
                snapshot.weakTopicSnapshot() == null ? null : snapshot.weakTopicSnapshot().focusTopicName(),
                snapshot.currentPlan() == null ? null : snapshot.currentPlan().getFocusDirection(),
                snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus(),
                snapshot.knowledgeDoc() == null ? null : firstNonBlank(snapshot.knowledgeDoc().getCategoryName(), snapshot.knowledgeDoc().getTitle()),
                snapshot.question() == null ? null : firstNonBlank(snapshot.question().getCategoryName(), snapshot.question().getJobDirection()),
                snapshot.interviewDetail() == null ? null : snapshot.interviewDetail().getDirection(),
                snapshot.recordingReview() == null ? null : snapshot.recordingReview().getDirection(),
                snapshot.copilotRealtimeSession() == null ? null : snapshot.copilotRealtimeSession().getJobTitle());
        String targetRole = firstNonBlank(
                snapshot.knowledgeDoc() != null && "jd".equalsIgnoreCase(snapshot.knowledgeDoc().getBusinessType())
                        ? snapshot.knowledgeDoc().getTitle()
                        : null,
                snapshot.question() == null ? null : snapshot.question().getJobDirection(),
                snapshot.application() == null ? null : snapshot.application().getJobTitle(),
                snapshot.applicationBoard() == null || snapshot.applicationBoard().focusApplication() == null
                        ? null
                        : snapshot.applicationBoard().focusApplication().getJobTitle(),
                snapshot.jobPrepSession() == null ? null : snapshot.jobPrepSession().getJobTitle(),
                snapshot.currentPlan() == null ? null : snapshot.currentPlan().getTargetRole(),
                snapshot.interviewDetail() == null ? null : snapshot.interviewDetail().getJobRole(),
                snapshot.recordingReview() == null ? null : snapshot.recordingReview().getJobRole(),
                snapshot.copilotRealtimeSession() == null ? null : snapshot.copilotRealtimeSession().getJobTitle());
        String techStack = firstNonBlank(
                snapshot.currentPlan() == null ? null : snapshot.currentPlan().getTechStack(),
                snapshot.knowledgeDoc() == null ? null : abbreviate(snapshot.knowledgeDoc().getSummary(), 48),
                snapshot.question() == null ? null : joinLimited(questionTags(snapshot.question()), 4, ", "),
                snapshot.interviewDetail() == null ? null : snapshot.interviewDetail().getTechStack(),
                snapshot.resume() == null ? null : joinLimited(snapshot.resume().getSkills(), 4, ", "),
                snapshot.application() == null ? null : joinLimited(snapshot.application().getJdKeywords(), 4, ", "),
                snapshot.applicationBoard() == null || snapshot.applicationBoard().focusApplication() == null
                        ? null
                        : joinLimited(snapshot.applicationBoard().focusApplication().getJdKeywords(), 4, ", "),
                snapshot.jobPrepSession() == null ? null : joinLimited(snapshot.jobPrepSession().getMatchedKeywords(), 4, ", "),
                snapshot.topicRetrospective() == null ? null : joinLimited(snapshot.topicRetrospective().getNextActions(), 2, ", "),
                snapshot.copilotRealtimeSession() == null ? null : joinLimited(
                        nullSafeList(sessionReviewActions(snapshot.copilotRealtimeSession())), 2, ", "));
        return new StudyPlanPayload(7, focusDirection, targetRole, techStack);
    }

    private List<String> sessionReviewActions(CopilotRealtimeSessionVO session) {
        if (session == null || session.getPostInterviewReview() == null) {
            return List.of();
        }
        return nullSafeList(session.getPostInterviewReview().getRecommendedActions());
    }

    private List<String> questionTags(QuestionVO question) {
        if (question == null || !StringUtils.hasText(question.getTags())) {
            return List.of();
        }
        return Arrays.stream(question.getTags().split("[,\\n]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private JobPrepDraftPayload resolveJobPrepDraftPayload(ContextSnapshot snapshot) {
        if (snapshot.jobPrepSession() != null) {
            JobPrepSessionVO session = snapshot.jobPrepSession();
            return new JobPrepDraftPayload(
                    session.getApplicationId(),
                    session.getResumeFileId(),
                    session.getCompany(),
                    session.getJobTitle(),
                    session.getJdText());
        }
        if (snapshot.application() != null) {
            JobApplicationVO application = snapshot.application();
            return new JobPrepDraftPayload(
                    application.getId(),
                    application.getResumeFileId(),
                    application.getCompany(),
                    application.getJobTitle(),
                    application.getJdText());
        }
        if (snapshot.applicationBoard() != null && snapshot.applicationBoard().focusApplication() != null) {
            JobApplicationVO application = snapshot.applicationBoard().focusApplication();
            return new JobPrepDraftPayload(
                    application.getId(),
                    application.getResumeFileId(),
                    application.getCompany(),
                    application.getJobTitle(),
                    application.getJdText());
        }
        if (snapshot.resume() != null) {
            ResumeFileVO resume = snapshot.resume();
            return new JobPrepDraftPayload(
                    null,
                    resume.getId(),
                    null,
                    null,
                    null);
        }
        return null;
    }

    private CopilotPrepDraftPayload resolveCopilotPrepDraftPayload(ContextSnapshot snapshot, String prompt) {
        if (snapshot.jobPrepSession() != null) {
            JobPrepSessionVO session = snapshot.jobPrepSession();
            return new CopilotPrepDraftPayload(
                    session.getApplicationId(),
                    session.getResumeFileId(),
                    session.getId(),
                    session.getCompany(),
                    session.getJobTitle(),
                    session.getJdText(),
                    prompt);
        }
        if (snapshot.application() != null) {
            JobApplicationVO application = snapshot.application();
            return new CopilotPrepDraftPayload(
                    application.getId(),
                    application.getResumeFileId(),
                    null,
                    application.getCompany(),
                    application.getJobTitle(),
                    application.getJdText(),
                    prompt);
        }
        if (snapshot.applicationBoard() != null && snapshot.applicationBoard().focusApplication() != null) {
            JobApplicationVO application = snapshot.applicationBoard().focusApplication();
            return new CopilotPrepDraftPayload(
                    application.getId(),
                    application.getResumeFileId(),
                    null,
                    application.getCompany(),
                    application.getJobTitle(),
                    application.getJdText(),
                    prompt);
        }
        return null;
    }

    private ResumeFollowUpDraftPayload resolveResumeFollowUpDraftPayload(
            ContextSnapshot snapshot, String summary, List<String> recommendations) {
        Long resumeId = null;
        if (snapshot.resume() != null) {
            resumeId = snapshot.resume().getId();
        } else if (snapshot.application() != null) {
            resumeId = snapshot.application().getResumeFileId();
        } else if (snapshot.applicationBoard() != null && snapshot.applicationBoard().focusApplication() != null) {
            resumeId = snapshot.applicationBoard().focusApplication().getResumeFileId();
        } else if (snapshot.jobPrepSession() != null) {
            resumeId = snapshot.jobPrepSession().getResumeFileId();
        }
        if (resumeId == null) {
            return null;
        }
        return new ResumeFollowUpDraftPayload(resumeId, summary, recommendations);
    }

    private RecordingReviewActionPayload resolveRecordingReviewActionPayload(
            ContextSnapshot snapshot, StudyPlanPayload studyPlanPayload) {
        RecordingReviewSessionVO recordingReview = snapshot.recordingReview();
        if (recordingReview == null || recordingReview.getId() == null) {
            return null;
        }
        String focusDirection = firstNonBlank(
                recordingReview.getDirection(),
                studyPlanPayload == null ? null : studyPlanPayload.focusDirection(),
                snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus());
        String targetRole = firstNonBlank(
                recordingReview.getJobRole(),
                studyPlanPayload == null ? null : studyPlanPayload.targetRole(),
                snapshot.jobPrepSession() == null ? null : snapshot.jobPrepSession().getJobTitle());
        String taskTitle = "录音复盘专项 | "
                + defaultText(firstItem(recordingReview.getWeakPoints()), defaultText(focusDirection, "表达修正"));
        String taskDescription = "基于本次录音复盘，优先处理 "
                + defaultText(joinLimited(recordingReview.getWeakPoints(), 2, "、"), "表达结构和案例支撑")
                + "。下一步先回听片段，再把建议动作带入一轮模拟或专项训练。";
        return new RecordingReviewActionPayload(
                recordingReview.getId(),
                focusDirection,
                targetRole,
                studyPlanPayload == null ? null : studyPlanPayload.techStack(),
                taskTitle,
                taskDescription,
                "/interview?workspace=recording-review&recordingReviewSessionId=" + recordingReview.getId());
    }

    private InterviewReviewActionPayload resolveInterviewReviewActionPayload(
            ContextSnapshot snapshot, StudyPlanPayload studyPlanPayload) {
        if (snapshot.interviewDetail() != null && snapshot.interviewDetail().getSessionId() != null) {
            InterviewDetailVO interviewDetail = snapshot.interviewDetail();
            List<String> weakTags = collectWeakPointTags(interviewDetail);
            String focusDirection = firstNonBlank(
                    interviewDetail.getDirection(),
                    studyPlanPayload == null ? null : studyPlanPayload.focusDirection(),
                    snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus());
            String targetRole = firstNonBlank(
                    interviewDetail.getJobRole(),
                    studyPlanPayload == null ? null : studyPlanPayload.targetRole(),
                    snapshot.jobPrepSession() == null ? null : snapshot.jobPrepSession().getJobTitle());
            String taskTitle = "面试复盘专项 | " + defaultText(firstItem(weakTags), defaultText(focusDirection, "低分点收紧"));
            int lowScoreCount = countLowScoreRecords(interviewDetail);
            String taskDescription = "基于本轮模拟面试，优先处理 "
                    + defaultText(joinLimited(weakTags, 2, "、"), "低分题的表达结构和追问深度")
                    + "。"
                    + (lowScoreCount > 0 ? " 当前共有 " + lowScoreCount + " 道低分题。" : "")
                    + " 下一步先执行 "
                    + defaultText(firstItem(interviewReviewRecommendations(interviewDetail)), "一轮定向复盘和专项训练")
                    + "。";
            return new InterviewReviewActionPayload(
                    interviewDetail.getSessionId(),
                    null,
                    focusDirection,
                    targetRole,
                    studyPlanPayload == null ? null : studyPlanPayload.techStack(),
                    taskTitle,
                    taskDescription,
                    "/interview/detail/" + interviewDetail.getSessionId());
        }
        if (snapshot.copilotRealtimeSession() != null && snapshot.copilotRealtimeSession().getId() != null) {
            CopilotRealtimeSessionVO session = snapshot.copilotRealtimeSession();
            List<String> weakPoints = session.getPostInterviewReview() == null
                    ? List.of()
                    : nullSafeList(session.getPostInterviewReview().getWeakPoints());
            String focusDirection = firstNonBlank(
                    session.getJobTitle(),
                    studyPlanPayload == null ? null : studyPlanPayload.focusDirection(),
                    snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus());
            String targetRole = firstNonBlank(
                    session.getJobTitle(),
                    studyPlanPayload == null ? null : studyPlanPayload.targetRole(),
                    snapshot.jobPrepSession() == null ? null : snapshot.jobPrepSession().getJobTitle());
            String taskTitle = "面后复盘专项 | "
                    + defaultText(firstItem(weakPoints), defaultText(session.getJobTitle(), "实时阶段收束"));
            String taskDescription = "基于本轮实时阶段复盘，优先处理 "
                    + defaultText(joinLimited(weakPoints, 2, "、"), "现场追问、卡壳点和表达缺口")
                    + "。下一步先执行 "
                    + defaultText(firstItem(copilotRealtimeRecommendations(session)), "一轮面后复盘和专项训练")
                    + "。";
            return new InterviewReviewActionPayload(
                    null,
                    session.getId(),
                    focusDirection,
                    targetRole,
                    studyPlanPayload == null ? null : studyPlanPayload.techStack(),
                    taskTitle,
                    taskDescription,
                    "/interview?workspace=copilot-live&copilotRealtimeSessionId=" + session.getId());
        }
        return null;
    }

    private TopicRetrospectiveActionPayload resolveTopicRetrospectiveActionPayload(
            ContextSnapshot snapshot, StudyPlanPayload studyPlanPayload) {
        ProfileTopicRetrospectiveVO retrospective = snapshot.topicRetrospective();
        if (retrospective == null || retrospective.getCategoryId() == null) {
            return null;
        }
        String focusDirection = firstNonBlank(
                retrospective.getCategoryName(),
                studyPlanPayload == null ? null : studyPlanPayload.focusDirection(),
                snapshot.abilityProfile() == null ? null : snapshot.abilityProfile().getSuggestedFocus());
        String taskTitle = "领域回顾专项 | " + defaultText(retrospective.getCategoryName(), defaultText(focusDirection, "训练收束"));
        String taskDescription = "基于当前领域回顾，优先处理 "
                + defaultText(joinLimited(retrospective.getRiskSignals(), 2, "、"), "阶段性风险")
                + "。下一步先执行 "
                + defaultText(firstItem(retrospective.getNextActions()), "一轮专项训练")
                + "。";
        return new TopicRetrospectiveActionPayload(
                retrospective.getCategoryId(),
                focusDirection,
                studyPlanPayload == null ? null : studyPlanPayload.targetRole(),
                studyPlanPayload == null ? null : studyPlanPayload.techStack(),
                taskTitle,
                taskDescription,
                resolveAnalyticsRetrospectivePath(retrospective.getCategoryId()));
    }

    private String resolveCoordinatorNextActionPath(ContextSnapshot snapshot) {
        if (snapshot.dashboardOverview() != null
                && snapshot.dashboardOverview().getNextAction() != null
                && StringUtils.hasText(snapshot.dashboardOverview().getNextAction().getPath())) {
            return snapshot.dashboardOverview().getNextAction().getPath().trim();
        }
        if (snapshot.recordingReview() != null) {
            return resolveRecordingReviewWorkspacePath(snapshot);
        }
        if (snapshot.topicRetrospective() != null) {
            return resolveAnalyticsRetrospectivePath(snapshot.topicRetrospective().getCategoryId());
        }
        if (snapshot.copilotRealtimeSession() != null) {
            String reviewActionPath = realtimeReviewNextActionPath(snapshot.copilotRealtimeSession());
            return StringUtils.hasText(reviewActionPath) ? reviewActionPath : resolveCopilotLiveWorkspacePath(snapshot);
        }
        if (snapshot.copilotPrepSession() != null) {
            return resolveCopilotLiveWorkspacePath(snapshot);
        }
        if (snapshot.jobPrepSession() != null) {
            return resolveJobPrepWorkspacePath(snapshot);
        }
        if (snapshot.application() != null && snapshot.application().getId() != null) {
            return "/applications/" + snapshot.application().getId();
        }
        if (snapshot.applicationBoard() != null
                && snapshot.applicationBoard().focusApplication() != null
                && snapshot.applicationBoard().focusApplication().getId() != null) {
            return "/applications/" + snapshot.applicationBoard().focusApplication().getId();
        }
        if (snapshot.wrongQuestion() != null && snapshot.wrongQuestion().getId() != null) {
            return "/wrong?wrongId=" + snapshot.wrongQuestion().getId();
        }
        if (snapshot.question() != null && snapshot.question().getId() != null) {
            return "/question?questionId=" + snapshot.question().getId();
        }
        if (snapshot.knowledgeDoc() != null) {
            String businessType = normalize(snapshot.knowledgeDoc().getBusinessType());
            if ("jd".equals(businessType)) {
                return "/interview?workspace=job-prep";
            }
            if ("resume".equals(businessType)) {
                return "/resume";
            }
            if (snapshot.knowledgeDoc().getId() != null) {
                return "/knowledge?docId=" + snapshot.knowledgeDoc().getId();
            }
            return "/knowledge";
        }
        if (snapshot.interviewDetail() != null && snapshot.interviewDetail().getSessionId() != null) {
            return "/interview/detail/" + snapshot.interviewDetail().getSessionId();
        }
        if (snapshot.resume() != null) {
            return "/resume";
        }
        if (snapshot.currentPlan() != null) {
            return "/study-plan";
        }
        if (snapshot.weakTopicSnapshot() != null && snapshot.weakTopicSnapshot().focusTopicId() != null) {
            return "/analytics?topic=" + snapshot.weakTopicSnapshot().focusTopicId();
        }
        if (snapshot.topicDetail() != null || snapshot.abilityProfile() != null) {
            return "/analytics";
        }
        return "/dashboard";
    }

    private String resolveStudyPlannerNextActionPath(ContextSnapshot snapshot) {
        if (snapshot.topicRetrospective() != null && snapshot.topicRetrospective().getCategoryId() != null) {
            return resolveAnalyticsRetrospectivePath(snapshot.topicRetrospective().getCategoryId());
        }
        if (snapshot.topicDetail() != null && snapshot.topicDetail().getCategoryId() != null) {
            return "/analytics?topic=" + snapshot.topicDetail().getCategoryId();
        }
        if (snapshot.wrongQuestion() != null && snapshot.wrongQuestion().getId() != null) {
            return "/wrong?wrongId=" + snapshot.wrongQuestion().getId();
        }
        if (snapshot.question() != null && snapshot.question().getId() != null) {
            return "/question?questionId=" + snapshot.question().getId();
        }
        if (snapshot.knowledgeDoc() != null && snapshot.knowledgeDoc().getCategoryId() != null) {
            return "/question?categoryId=" + snapshot.knowledgeDoc().getCategoryId();
        }
        if (snapshot.knowledgeDoc() != null) {
            if (snapshot.knowledgeDoc().getId() != null) {
                return "/knowledge?docId=" + snapshot.knowledgeDoc().getId();
            }
            return "/question";
        }
        if (snapshot.weakTopicSnapshot() != null && snapshot.weakTopicSnapshot().focusTopicId() != null) {
            return "/analytics?topic=" + snapshot.weakTopicSnapshot().focusTopicId();
        }
        if (snapshot.weakTopicSnapshot() != null || snapshot.abilityProfile() != null) {
            return "/analytics";
        }
        return "/study-plan";
    }

    private String resolveAnalyticsRetrospectivePath(Long categoryId) {
        if (categoryId == null) {
            return "/analytics";
        }
        return "/analytics?topic=" + categoryId + "&retrospective=1";
    }

    private String resolveJobPrepWorkspacePath(ContextSnapshot snapshot) {
        if (snapshot.jobPrepSession() != null && snapshot.jobPrepSession().getId() != null) {
            return "/interview?workspace=job-prep&jobPrepSessionId=" + snapshot.jobPrepSession().getId();
        }
        return "/interview?workspace=job-prep";
    }

    private String resolveRecordingReviewWorkspacePath(ContextSnapshot snapshot) {
        if (snapshot.recordingReview() != null && snapshot.recordingReview().getId() != null) {
            return "/interview?workspace=recording-review&recordingReviewSessionId=" + snapshot.recordingReview().getId();
        }
        return "/interview?workspace=recording-review";
    }

    private String resolveCopilotPrepWorkspacePath(ContextSnapshot snapshot) {
        if (snapshot.copilotPrepSession() != null && snapshot.copilotPrepSession().getId() != null) {
            return "/interview?workspace=copilot-prep&copilotPrepSessionId=" + snapshot.copilotPrepSession().getId();
        }
        if (snapshot.jobPrepSession() != null && snapshot.jobPrepSession().getId() != null) {
            return "/interview?workspace=copilot-prep&jobPrepSessionId=" + snapshot.jobPrepSession().getId();
        }
        return "/interview?workspace=copilot-prep";
    }

    private String resolveCopilotLiveWorkspacePath(ContextSnapshot snapshot) {
        if (snapshot.copilotRealtimeSession() != null && snapshot.copilotRealtimeSession().getId() != null) {
            return "/interview?workspace=copilot-live&copilotRealtimeSessionId=" + snapshot.copilotRealtimeSession().getId();
        }
        if (snapshot.copilotPrepSession() != null && snapshot.copilotPrepSession().getId() != null) {
            return "/interview?workspace=copilot-live&copilotPrepSessionId=" + snapshot.copilotPrepSession().getId();
        }
        return "/interview?workspace=copilot-live";
    }

    private String resolveResumeWorkspacePath(ContextSnapshot snapshot) {
        if (snapshot.resume() != null && snapshot.resume().getId() != null) {
            return "/resume?resumeId=" + snapshot.resume().getId();
        }
        if (snapshot.application() != null && snapshot.application().getResumeFileId() != null) {
            return "/resume?resumeId=" + snapshot.application().getResumeFileId();
        }
        if (snapshot.applicationBoard() != null
                && snapshot.applicationBoard().focusApplication() != null
                && snapshot.applicationBoard().focusApplication().getResumeFileId() != null) {
            return "/resume?resumeId=" + snapshot.applicationBoard().focusApplication().getResumeFileId();
        }
        if (snapshot.jobPrepSession() != null && snapshot.jobPrepSession().getResumeFileId() != null) {
            return "/resume?resumeId=" + snapshot.jobPrepSession().getResumeFileId();
        }
        return "/resume";
    }

    private String resolveRunNextActionPath(String basePath, String agentType, ContextSnapshot snapshot) {
        if (snapshot.providerConfigs() == null) {
            return basePath;
        }
        String providerGateStatus = resolveProviderGateStatus(resolveProviderGates(agentType, snapshot.providerConfigs(), snapshot));
        return "blocked".equals(providerGateStatus) ? "/settings?tab=providers" : basePath;
    }

    private String resolveRunNextActionLabel(AgentRun run, String providerGateStatus) {
        return resolveNextActionLabel(run.getAgentType(), run.getNextActionPath(), providerGateStatus);
    }

    private java.util.Optional<StudyPlanCurrentVO.StudyPlanTaskVO> firstPendingPlanTask(StudyPlanCurrentVO currentPlan) {
        return nullSafeList(currentPlan.getTasks()).stream()
                .filter(task -> !"completed".equals(normalize(task.getStatus())))
                .sorted((left, right) -> {
                    int dayCompare = Integer.compare(defaultInt(left.getDayIndex()), defaultInt(right.getDayIndex()));
                    if (dayCompare != 0) {
                        return dayCompare;
                    }
                    return Long.compare(
                            left.getId() == null ? Long.MAX_VALUE : left.getId(),
                            right.getId() == null ? Long.MAX_VALUE : right.getId());
                })
                .findFirst();
    }

    private ApplicationBoardSnapshot buildApplicationBoardSnapshot(List<JobApplicationVO> board) {
        List<JobApplicationVO> safeBoard = nullSafeList(board);
        if (safeBoard.isEmpty()) {
            return null;
        }
        List<JobApplicationVO> sortedBoard = safeBoard.stream()
                .sorted((left, right) -> {
                    int priorityCompare = Integer.compare(
                            applicationStatusPriority(left == null ? null : left.getStatus()),
                            applicationStatusPriority(right == null ? null : right.getStatus()));
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    BigDecimal leftScore = left == null || left.getMatchScore() == null ? BigDecimal.ZERO : left.getMatchScore();
                    BigDecimal rightScore = right == null || right.getMatchScore() == null ? BigDecimal.ZERO : right.getMatchScore();
                    int scoreCompare = rightScore.compareTo(leftScore);
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    LocalDateTime leftTime = left == null ? null : left.getUpdateTime();
                    LocalDateTime rightTime = right == null ? null : right.getUpdateTime();
                    if (leftTime == null && rightTime == null) {
                        return 0;
                    }
                    if (leftTime == null) {
                        return 1;
                    }
                    if (rightTime == null) {
                        return -1;
                    }
                    return rightTime.compareTo(leftTime);
                })
                .toList();
        int activeCount = (int) sortedBoard.stream()
                .filter(item -> isActiveApplicationStatus(item == null ? null : item.getStatus()))
                .count();
        int offerCount = (int) sortedBoard.stream()
                .filter(item -> "offer".equals(normalize(item == null ? null : item.getStatus())))
                .count();
        int rejectedCount = (int) sortedBoard.stream()
                .filter(item -> "rejected".equals(normalize(item == null ? null : item.getStatus())))
                .count();
        return new ApplicationBoardSnapshot(
                sortedBoard,
                sortedBoard.get(0),
                sortedBoard.size(),
                activeCount,
                offerCount,
                rejectedCount);
    }

    private AgentRunVO buildVo(AgentRun run) {
        JsonNode payload = readPayload(run.getResultPayloadJson());
        ExecutionResult executionResult = readObject(run.getExecutionResultJson(), ExecutionResult.class);
        List<AgentRunVO.ProviderGateVO> providerGates = resolveProviderGates(run);
        String providerGateStatus = resolveProviderGateStatus(providerGates);
        return AgentRunVO.builder()
                .id(run.getId())
                .agentType(run.getAgentType())
                .triggerSource(run.getTriggerSource())
                .status(run.getStatus())
                .title(run.getTitle())
                .summary(run.getSummary())
                .userPrompt(run.getUserPrompt())
                .contextRefs(readList(run.getContextRefsJson()))
                .streamMode(run.getStreamMode())
                .recommendations(readPayloadArray(payload, "recommendations"))
                .checkpoints(readPayloadArray(payload, "checkpoints"))
                .nextActionPath(run.getNextActionPath())
                .nextActionLabel(resolveRunNextActionLabel(run, providerGateStatus))
                .requiresApproval(Integer.valueOf(1).equals(run.getRequiresApproval()))
                .approvalActionType(run.getApprovalActionType())
                .approvalSummary(run.getApprovalSummary())
                .decisionNote(run.getDecisionNote())
                .executionSummary(executionResult == null ? null : executionResult.summary())
                .executionActionLabel(executionResult == null ? null : executionResult.actionLabel())
                .executionActionPath(executionResult == null ? null : executionResult.nextActionPath())
                .approvalStage(resolveApprovalStage(run))
                .providerGateStatus(providerGateStatus)
                .providerGateSummary(buildProviderGateSummary(providerGates, providerGateStatus))
                .timeline(buildTimeline(run, executionResult))
                .providerGates(providerGates)
                .updateTime(run.getUpdateTime())
                .build();
    }

    private String resolveApprovalStage(AgentRun run) {
        if (!Integer.valueOf(1).equals(run.getRequiresApproval())) {
            return "not_required";
        }
        return switch (normalize(run.getStatus())) {
            case "pending_approval" -> "waiting";
            case "approved" -> "approved";
            case "rejected" -> "rejected";
            case "canceled" -> "canceled";
            default -> "completed";
        };
    }

    private List<AgentRunVO.TimelineItemVO> buildTimeline(AgentRun run, ExecutionResult executionResult) {
        List<AgentRunVO.TimelineItemVO> timeline = new ArrayList<>();
        LocalDateTime createTime = run.getCreateTime() == null ? run.getUpdateTime() : run.getCreateTime();
        timeline.add(AgentRunVO.TimelineItemVO.builder()
                .key("request_received")
                .stepType(StepType.RETRIEVE.value())
                .title("任务已创建")
                .description("已按 " + defaultText(run.getTriggerSource(), "当前来源") + " 发起 "
                        + defaultText(run.getAgentType(), "agent") + " 任务。")
                .status("completed")
                .timestamp(createTime)
                .build());
        timeline.add(AgentRunVO.TimelineItemVO.builder()
                .key("analysis_ready")
                .stepType(resolveAnalysisStepType(run))
                .title("分析结果已整理")
                .description(defaultText(run.getSummary(), "当前 run 已生成结构化建议。"))
                .status("completed")
                .timestamp(run.getUpdateTime())
                .build());
        if (Integer.valueOf(1).equals(run.getRequiresApproval())) {
            timeline.add(AgentRunVO.TimelineItemVO.builder()
                    .key("approval_gate")
                    .stepType(StepType.WAIT_APPROVAL.value())
                    .title("审批门控")
                    .description(defaultText(run.getApprovalSummary(), "当前写操作需要审批后才能执行。"))
                    .status(resolveApprovalTimelineStatus(run.getStatus()))
                    .timestamp(run.getUpdateTime())
                    .build());
        }
        if (executionResult != null && StringUtils.hasText(executionResult.summary())) {
            timeline.add(AgentRunVO.TimelineItemVO.builder()
                    .key("execution_result")
                    .stepType(resolveExecutionStepType(run))
                    .title("结果已执行")
                    .description(executionResult.summary())
                    .status("completed")
                    .timestamp(run.getUpdateTime())
                    .build());
        } else if ("completed".equals(normalize(run.getStatus()))) {
            timeline.add(AgentRunVO.TimelineItemVO.builder()
                    .key("result_delivered")
                    .stepType(resolveExecutionStepType(run))
                    .title("结果已交付")
                    .description("当前 run 已完成，可继续进入下一步消费结果。")
                    .status("completed")
                    .timestamp(run.getUpdateTime())
                    .build());
        }
        if (StringUtils.hasText(run.getNextActionPath())) {
            timeline.add(AgentRunVO.TimelineItemVO.builder()
                    .key("next_action")
                    .stepType(resolveNextActionStepType(run))
                    .title("下一步动作")
                    .description("建议继续前往 " + run.getNextActionPath() + " 消费结果。")
                    .status("ready")
                    .timestamp(run.getUpdateTime())
                    .build());
        }
        return timeline;
    }

    private String resolveApprovalTimelineStatus(String runStatus) {
        return switch (normalize(runStatus)) {
            case "pending_approval" -> "waiting";
            case "rejected" -> "rejected";
            case "canceled" -> "canceled";
            default -> "completed";
        };
    }

    private String resolveAnalysisStepType(AgentRun run) {
        return switch (normalize(run.getAgentType())) {
            case "recording_review" -> StepType.SCORE.value();
            case "realtime_copilot" -> StepType.PREPARE_REALTIME.value();
            default -> StepType.ANALYZE.value();
        };
    }

    private String resolveExecutionStepType(AgentRun run) {
        return switch (normalize(run.getApprovalActionType())) {
            case "refresh_study_plan",
                 "save_interview_review_action",
                 "save_topic_retrospective_action",
                 "save_recording_review_action" -> StepType.SCHEDULE_REVIEW.value();
            case "save_copilot_prep_draft" -> StepType.PREPARE_REALTIME.value();
            default -> StepType.UPDATE_PROFILE.value();
        };
    }

    private String resolveNextActionStepType(AgentRun run) {
        if ("realtime_copilot".equals(normalize(run.getAgentType()))) {
            return StepType.PREPARE_REALTIME.value();
        }
        if ("recording_review".equals(normalize(run.getAgentType()))
                && "pending_approval".equals(normalize(run.getStatus()))) {
            return StepType.SCHEDULE_REVIEW.value();
        }
        if ("study_planner".equals(normalize(run.getAgentType()))
                || "interview_review".equals(normalize(run.getAgentType()))) {
            return StepType.SCHEDULE_REVIEW.value();
        }
        return StepType.ANALYZE.value();
    }

    private List<AgentRunVO.ProviderGateVO> resolveProviderGates(AgentRun run) {
        return resolveProviderGates(run.getAgentType(), loadProviderConfigs(), inferAgentPhase(run));
    }

    private List<AgentRunVO.ProviderGateVO> resolveProviderGates(String agentType, List<UserProviderConfigItemVO> configs) {
        return resolveProviderGates(agentType, configs, (String) null);
    }

    private List<AgentRunVO.ProviderGateVO> resolveProviderGates(
            String agentType,
            List<UserProviderConfigItemVO> configs,
            ContextSnapshot snapshot) {
        return resolveProviderGates(agentType, configs, inferAgentPhase(agentType, snapshot));
    }

    private List<AgentRunVO.ProviderGateVO> resolveProviderGates(
            String agentType,
            List<UserProviderConfigItemVO> configs,
            String phase) {
        List<ProviderRequirement> requirements = providerRequirements(agentType, phase);
        if (requirements.isEmpty()) {
            return List.of();
        }
        Map<String, UserProviderConfigItemVO> configMap = new LinkedHashMap<>();
        for (UserProviderConfigItemVO item : configs == null ? List.<UserProviderConfigItemVO>of() : configs) {
            if (item != null && StringUtils.hasText(item.getScope())) {
                configMap.put(normalize(item.getScope()), item);
            }
        }
        List<AgentRunVO.ProviderGateVO> gates = new ArrayList<>();
        for (ProviderRequirement requirement : requirements) {
            UserProviderConfigItemVO item = configMap.get(requirement.scope());
            gates.add(AgentRunVO.ProviderGateVO.builder()
                    .scope(requirement.scope())
                    .label(item == null ? requirement.label() : item.getLabel())
                    .status(item == null ? "missing" : item.getStatus())
                    .statusMessage(item == null ? requirement.missingMessage() : item.getStatusMessage())
                    .required(requirement.required())
                    .build());
        }
        return gates;
    }

    private String inferAgentPhase(String agentType, ContextSnapshot snapshot) {
        if (!"realtime_copilot".equals(normalize(agentType))) {
            return "default";
        }
        if (snapshot == null) {
            return "default";
        }
        CopilotRealtimeSessionVO realtimeSession = snapshot.copilotRealtimeSession();
        if (realtimeSession != null) {
            String status = normalize(realtimeSession.getStatus());
            return "completed".equals(status) ? "post_review" : "live";
        }
        if (snapshot.copilotPrepSession() != null || snapshot.jobPrepSession() != null
                || snapshot.application() != null || snapshot.applicationBoard() != null || snapshot.resume() != null) {
            return "prep";
        }
        return "prep";
    }

    private String inferAgentPhase(AgentRun run) {
        String agentType = normalize(run.getAgentType());
        if (!"realtime_copilot".equals(agentType)) {
            return "default";
        }
        List<String> contextRefs = readList(run.getContextRefsJson());
        boolean hasRealtimeContext = contextRefs.stream().anyMatch(ref -> ref.regionMatches(true, 0, "interview:copilot-realtime", 0,
                "interview:copilot-realtime".length()));
        if (hasRealtimeContext) {
            String nextActionPath = defaultText(run.getNextActionPath(), "");
            return nextActionPath.contains("agentType=interview_review") ? "post_review" : "live";
        }
        return "prep";
    }

    private List<UserProviderConfigItemVO> loadProviderConfigs() {
        try {
            List<UserProviderConfigItemVO> configs = userProviderConfigService.listCurrentUserConfigs();
            return configs == null ? List.of() : configs;
        } catch (Exception ex) {
            log.warn("Failed to resolve user provider configs for agent gating", ex);
            return List.of();
        }
    }

    private String resolveProviderGateStatus(List<AgentRunVO.ProviderGateVO> providerGates) {
        if (providerGates.isEmpty()) {
            return "not_applicable";
        }
        boolean hasRequiredGap = providerGates.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRequired()))
                .anyMatch(item -> !isProviderAvailable(item.getStatus()));
        if (hasRequiredGap) {
            return "blocked";
        }
        boolean hasOptionalGap = providerGates.stream().anyMatch(item -> !isProviderAvailable(item.getStatus()));
        return hasOptionalGap ? "degraded" : "ready";
    }

    private boolean isProviderAvailable(String status) {
        String normalized = normalize(status);
        return "ready".equals(normalized) || "saved".equals(normalized);
    }

    private String resolveNextActionLabel(String agentType, String nextActionPath, String providerGateStatus) {
        String normalizedPath = defaultText(nextActionPath, "");
        if ("/settings?tab=providers".equals(normalizedPath)
                || ("blocked".equals(normalize(providerGateStatus)) && !StringUtils.hasText(normalizedPath))) {
            return "前往 Provider 设置";
        }
        if (normalizedPath.contains("agentType=interview_review")) {
            return "发起面后复盘";
        }
        return switch (normalize(agentType)) {
            case "study_planner" -> studyPlannerNextActionLabel(normalizedPath);
            case "job_prep" -> "前往 JD 备面";
            case "recording_review" -> "前往录音复盘";
            case "interview_review" -> normalizedPath.startsWith("/interview/detail/") ? "前往面试详情" : "前往面试复盘";
            case "resume_coach" -> "前往简历页";
            case "application_strategist" -> normalizedPath.startsWith("/applications/") ? "前往投递详情" : "前往投递页";
            case "realtime_copilot" -> realtimeCopilotNextActionLabel(normalizedPath);
            case "coordinator" -> coordinatorNextActionLabel(normalizedPath);
            default -> "前往下一步";
        };
    }

    private String realtimeCopilotNextActionLabel(String nextActionPath) {
        if (nextActionPath.startsWith("/interview?workspace=copilot-live")) {
            return "前往实时 Copilot";
        }
        if (nextActionPath.startsWith("/interview?workspace=copilot-prep")) {
            return "前往 Copilot Prep";
        }
        if (nextActionPath.startsWith("/interview?workspace=recording-review")) {
            return "前往录音复盘";
        }
        return "前往 Copilot";
    }

    private String studyPlannerNextActionLabel(String nextActionPath) {
        if (nextActionPath.startsWith("/analytics?topic=") && nextActionPath.contains("retrospective=1")) {
            return "前往领域回顾";
        }
        if (nextActionPath.startsWith("/analytics?topic=")) {
            return "前往主题画像";
        }
        if (nextActionPath.startsWith("/analytics")) {
            return "前往能力画像";
        }
        if (nextActionPath.startsWith("/wrong?wrongId=") || nextActionPath.startsWith("/wrong")) {
            return "前往错题本";
        }
        if (nextActionPath.startsWith("/question?questionId=")) {
            return "前往题目详情";
        }
        if (nextActionPath.startsWith("/question")) {
            return "前往题库训练";
        }
        if (nextActionPath.startsWith("/knowledge?docId=")) {
            return "前往知识资料";
        }
        if (nextActionPath.startsWith("/knowledge")) {
            return "前往知识库";
        }
        return "前往训练计划";
    }

    private String coordinatorNextActionLabel(String nextActionPath) {
        if (nextActionPath.startsWith("/analytics?topic=") && nextActionPath.contains("retrospective=1")) {
            return "前往领域回顾";
        }
        if (nextActionPath.startsWith("/analytics?topic=")) {
            return "前往主题画像";
        }
        if (nextActionPath.startsWith("/analytics")) {
            return "前往能力画像";
        }
        if (nextActionPath.startsWith("/study-plan")) {
            return "前往训练计划";
        }
        if (nextActionPath.startsWith("/wrong?wrongId=") || nextActionPath.startsWith("/wrong")) {
            return "前往错题本";
        }
        if (nextActionPath.startsWith("/question?questionId=")) {
            return "前往题目详情";
        }
        if (nextActionPath.startsWith("/question")) {
            return "前往题库训练";
        }
        if (nextActionPath.startsWith("/knowledge?docId=")) {
            return "前往知识资料";
        }
        if (nextActionPath.startsWith("/knowledge")) {
            return "前往知识库";
        }
        if (nextActionPath.startsWith("/resume")) {
            return "前往简历页";
        }
        if (nextActionPath.startsWith("/applications/")) {
            return "前往投递详情";
        }
        if (nextActionPath.startsWith("/applications")) {
            return "前往投递页";
        }
        if (nextActionPath.startsWith("/interview/detail/")) {
            return "前往面试详情";
        }
        if (nextActionPath.startsWith("/interview?workspace=job-prep")) {
            return "前往 JD 备面";
        }
        if (nextActionPath.startsWith("/interview?workspace=recording-review")) {
            return "前往录音复盘";
        }
        if (nextActionPath.startsWith("/interview?workspace=copilot-prep")) {
            return "前往 Copilot Prep";
        }
        if (nextActionPath.startsWith("/interview?workspace=copilot-live")) {
            return "前往实时 Copilot";
        }
        if (nextActionPath.startsWith("/interview")) {
            return "前往模拟面试";
        }
        return "前往工作台";
    }

    private String buildProviderGateSummary(List<AgentRunVO.ProviderGateVO> providerGates, String overallStatus) {
        if (providerGates.isEmpty()) {
            return "当前 run 不依赖额外 provider gating。";
        }
        long missingCount = providerGates.stream().filter(item -> !isProviderAvailable(item.getStatus())).count();
        return switch (overallStatus) {
            case "blocked" -> "当前有 " + missingCount + " 项关键 provider 还没就绪，相关动作会被阻断或降级。";
            case "degraded" -> "当前关键 provider 已基本就绪，但仍有 " + missingCount + " 项依赖未完全可用。";
            default -> "当前相关 provider 依赖已基本就绪。";
        };
    }

    private List<String> unavailableProviderLabels(List<AgentRunVO.ProviderGateVO> providerGates, boolean requiredOnly) {
        return providerGates.stream()
                .filter(item -> !requiredOnly || Boolean.TRUE.equals(item.getRequired()))
                .filter(item -> !isProviderAvailable(item.getStatus()))
                .map(item -> defaultText(item.getLabel(), item.getScope()))
                .toList();
    }

    private List<ProviderRequirement> providerRequirements(String agentType, String phase) {
        String normalized = normalize(agentType);
        String normalizedPhase = normalize(phase);
        List<ProviderRequirement> requirements = new ArrayList<>();
        requirements.add(new ProviderRequirement("llm", "主模型", true, "还没有保存主模型配置。"));
        switch (normalized) {
            case "recording_review" -> {
                requirements.add(new ProviderRequirement("asr", "语音识别", false, "语音识别未配置时，音频上传会被禁用，但文字 transcript 模式仍可继续。"));
                requirements.add(new ProviderRequirement("oss", "对象存储", false, "长音频存储能力未配置，上传能力可能受限。"));
            }
            case "job_prep" -> requirements.add(new ProviderRequirement("search", "联网搜索", false, "联网搜索未配置，公司与岗位背景研究会降级。"));
            case "realtime_copilot" -> {
                if ("live".equals(normalizedPhase)) {
                    requirements.add(new ProviderRequirement("asr", "语音识别", true, "实时 Copilot 至少需要语音识别配置。"));
                    requirements.add(new ProviderRequirement("search", "联网搜索", true, "实时 Copilot 需要联网搜索支持背景检索。"));
                    requirements.add(new ProviderRequirement("voiceprint", "声纹识别", false, "声纹识别未配置，说话人区分会降级。"));
                } else if ("prep".equals(normalizedPhase)) {
                    requirements.add(new ProviderRequirement("search", "联网搜索", false, "联网搜索未配置，Copilot Prep 的公司与岗位研究会降级。"));
                    requirements.add(new ProviderRequirement("asr", "语音识别", false, "语音识别未配置，后续进入实时阶段前仍需补齐。"));
                    requirements.add(new ProviderRequirement("voiceprint", "声纹识别", false, "声纹识别未配置，后续说话人区分会降级。"));
                }
            }
            default -> {
            }
        }
        return requirements;
    }

    private RunBlueprint blueprint(String title, String summary, List<String> recommendations, List<String> checkpoints,
                                   String nextActionPath, boolean requiresApproval, String approvalActionType,
                                   String approvalSummary, String approvalPayloadJson) {
        return new RunBlueprint(title, summary, recommendations, checkpoints, nextActionPath, requiresApproval,
                approvalActionType, approvalSummary, approvalPayloadJson);
    }

    private List<String> mergeRecommendations(List<String>... groups) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (List<String> group : groups) {
            if (group != null) {
                merged.addAll(group);
            }
        }
        return merged.stream().limit(5).toList();
    }

    private List<String> contextRefsText(List<String> contextRefs, String prefix) {
        if (contextRefs.isEmpty()) {
            return List.of();
        }
        return List.of(prefix + " " + String.join("、", contextRefs.stream().limit(3).toList()) + "。");
    }

    private String writePayload(List<String> recommendations, List<String> checkpoints) {
        return writeObject(new ResultPayload(recommendations, checkpoints), "{\"recommendations\":[],\"checkpoints\":[]}");
    }

    private String writeObject(Object value, String fallback) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object for agent run", e);
            return fallback;
        }
    }

    private JsonNode readPayload(String raw) {
        if (!StringUtils.hasText(raw)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse agent payload", e);
            return objectMapper.createObjectNode();
        }
    }

    private List<String> readPayloadArray(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        });
        return result;
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            log.warn("Failed to write context refs", e);
            return "[]";
        }
    }

    private List<String> readList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to read context refs", e);
            return List.of();
        }
    }

    private <T> T readObject(String raw, Class<T> type) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse agent run object", e);
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String abbreviate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit) + "...";
    }

    private boolean hasContext(List<String> contextRefs, String target) {
        return contextRefs.stream().anyMatch(target::equalsIgnoreCase);
    }

    private Long findContextRefId(List<String> contextRefs, String prefix) {
        return contextRefs.stream()
                .filter(ref -> ref.regionMatches(true, 0, prefix, 0, prefix.length()))
                .map(ref -> ref.substring(prefix.length()))
                .map(this::parseLong)
                .filter(id -> id != null)
                .findFirst()
                .orElse(null);
    }

    private String findContextRefValue(List<String> contextRefs, String prefix) {
        return contextRefs.stream()
                .filter(ref -> ref.regionMatches(true, 0, prefix, 0, prefix.length()))
                .map(ref -> trimToNull(ref.substring(prefix.length())))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private Long resolveTopicIdByName(AbilityProfileVO profile, String topicName) {
        if (profile == null || !StringUtils.hasText(topicName)) {
            return null;
        }
        String normalizedTopicName = topicName.trim();
        return nullSafeList(profile.getCategoryAbilities()).stream()
                .filter(item -> item != null && item.getCategoryId() != null)
                .filter(item -> normalizedTopicName.equalsIgnoreCase(trimToNull(item.getCategoryName())))
                .map(CategoryAbilityVO::getCategoryId)
                .findFirst()
                .orElse(null);
    }

    private Long parseLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private <T> T loadOptional(String label, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException ex) {
            log.warn("Skip stale agent context {}: {}", label, ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Failed to resolve agent context {}", label, ex);
            return null;
        }
    }

    private int countLowScoreRecords(InterviewDetailVO interviewDetail) {
        if (interviewDetail.getRecords() == null) {
            return 0;
        }
        return (int) interviewDetail.getRecords().stream()
                .filter(record -> Boolean.TRUE.equals(record.getIsLowScore()))
                .count();
    }

    private List<String> collectWeakPointTags(InterviewDetailVO interviewDetail) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (interviewDetail.getRecords() != null) {
            interviewDetail.getRecords().forEach(record -> tags.addAll(nullSafeList(record.getWeakPointTags())));
        }
        return new ArrayList<>(tags);
    }

    private <T> List<T> nullSafeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> List<T> limit(List<T> values, int maxSize) {
        List<T> safeValues = nullSafeList(values);
        return safeValues.size() <= maxSize ? safeValues : safeValues.subList(0, maxSize);
    }

    private String firstItem(List<String> values) {
        return limit(values, 1).stream().findFirst().orElse(null);
    }

    private String joinLimited(List<String> values, int limit, String delimiter) {
        List<String> safeValues = nullSafeList(values).stream()
                .filter(StringUtils::hasText)
                .limit(limit)
                .map(String::trim)
                .toList();
        return safeValues.isEmpty() ? null : String.join(delimiter, safeValues);
    }

    private String difficultyLabel(String value) {
        return switch (normalize(value)) {
            case "hard" -> "高强度";
            case "medium" -> "中强度";
            default -> "基础巩固";
        };
    }

    private String applicationStatusLabel(String value) {
        return switch (normalize(value)) {
            case "saved" -> "待投递";
            case "applied" -> "已投递";
            case "written" -> "笔试 / 作业";
            case "interview" -> "面试中";
            case "offer" -> "Offer";
            case "rejected" -> "已淘汰";
            default -> "待推进";
        };
    }

    private int applicationStatusPriority(String value) {
        return switch (normalize(value)) {
            case "interview" -> 0;
            case "written" -> 1;
            case "applied" -> 2;
            case "saved" -> 3;
            case "offer" -> 4;
            case "rejected" -> 5;
            default -> 99;
        };
    }

    private boolean isActiveApplicationStatus(String value) {
        String normalized = normalize(value);
        return "applied".equals(normalized) || "written".equals(normalized) || "interview".equals(normalized);
    }

    private String formatNumber(Number value) {
        if (value == null) {
            return null;
        }
        double number = value.doubleValue();
        if (Math.rint(number) == number) {
            return String.valueOf((long) number);
        }
        return String.format(Locale.ROOT, "%.1f", number);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean matchesRunFilter(AgentRun run, String agentType, String status, String triggerSource,
                                     String approvalStage, String providerGateStatus) {
        AgentRunVO runVo = buildVo(run);
        return matchesFilterValue(run == null ? null : run.getAgentType(), agentType)
                && matchesFilterValue(run == null ? null : run.getStatus(), status)
                && matchesFilterValue(run == null ? null : run.getTriggerSource(), triggerSource)
                && matchesFilterValue(runVo.getApprovalStage(), approvalStage)
                && matchesFilterValue(runVo.getProviderGateStatus(), providerGateStatus);
    }

    private boolean matchesFilterValue(String actual, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return normalize(actual).equals(normalize(expected));
    }

    private String resolveSavedPlanTaskPath(StudyPlanCurrentVO plan, String module, String actionPath, String taskTitle) {
        if (plan == null || plan.getTasks() == null || plan.getTasks().isEmpty()) {
            return "/study-plan";
        }
        for (StudyPlanCurrentVO.StudyPlanTaskVO task : plan.getTasks()) {
            if (task == null) {
                continue;
            }
            if (module.equals(normalize(task.getModule()))
                    && normalize(task.getActionPath()).equals(normalize(actionPath))
                    && normalize(task.getTitle()).equals(normalize(taskTitle))) {
                return "/study-plan?taskId=" + task.getId();
            }
        }
        for (StudyPlanCurrentVO.StudyPlanTaskVO task : plan.getTasks()) {
            if (task == null) {
                continue;
            }
            if (module.equals(normalize(task.getModule()))
                    && normalize(task.getActionPath()).equals(normalize(actionPath))) {
                return "/study-plan?taskId=" + task.getId();
            }
        }
        return "/study-plan";
    }

    private record RunBlueprint(String title, String summary, List<String> recommendations, List<String> checkpoints,
                                String nextActionPath, boolean requiresApproval, String approvalActionType,
                                String approvalSummary, String approvalPayloadJson) {
    }

    private record ResultPayload(List<String> recommendations, List<String> checkpoints) {
    }

    private record StudyPlanPayload(Integer durationDays, String focusDirection, String targetRole, String techStack) {
    }

    private record ExecutionResult(String summary, String actionLabel, String nextActionPath) {
    }

    private record JobPrepDraftPayload(Long applicationId, Long resumeId, String company, String jobTitle, String jdText) {
    }

    private record CopilotPrepDraftPayload(Long applicationId, Long resumeId, Long jobPrepSessionId, String company,
                                           String jobTitle, String jdText, String notes) {
    }

    private record ApplicationStrategyPayload(Long applicationId, String summary, List<String> recommendations) {
    }

    private record ResumeFollowUpDraftPayload(Long resumeId, String summary, List<String> recommendations) {
    }

    private record InterviewReviewActionPayload(Long interviewSessionId, Long copilotRealtimeSessionId,
                                                String focusDirection, String targetRole, String techStack,
                                                String taskTitle, String taskDescription, String actionPath) {
    }

    private record RecordingReviewActionPayload(Long recordingReviewSessionId, String focusDirection, String targetRole,
                                                String techStack, String taskTitle, String taskDescription,
                                                String actionPath) {
    }

    private record TopicRetrospectiveActionPayload(Long categoryId, String focusDirection, String targetRole,
                                                   String techStack, String taskTitle, String taskDescription,
                                                   String actionPath) {
    }

    private record ProviderRequirement(String scope, String label, boolean required, String missingMessage) {
    }

    private record ContextSnapshot(AbilityProfileVO abilityProfile, ProfileTopicDetailVO topicDetail,
                                   ProfileTopicRetrospectiveVO topicRetrospective,
                                   WeakTopicSnapshot weakTopicSnapshot,
                                   DashboardOverviewVO dashboardOverview,
                                   StudyPlanCurrentVO currentPlan,
                                   InterviewDetailVO interviewDetail, RecordingReviewSessionVO recordingReview,
                                   KnowledgeDocVO knowledgeDoc,
                                   QuestionVO question, WrongQuestionVO wrongQuestion,
                                   ResumeFileVO resume, JobApplicationVO application,
                                   ApplicationBoardSnapshot applicationBoard,
                                   JobPrepSessionVO jobPrepSession,
                                   CopilotPrepSessionVO copilotPrepSession,
                                   CopilotRealtimeSessionVO copilotRealtimeSession,
                                   List<UserProviderConfigItemVO> providerConfigs) {
    }

    private record ApplicationBoardSnapshot(List<JobApplicationVO> applications, JobApplicationVO focusApplication,
                                            Integer totalCount, Integer activeCount, Integer offerCount,
                                            Integer rejectedCount) {
    }

    private record WeakTopicSnapshot(List<CategoryAbilityVO> topics, Long focusTopicId, String focusTopicName,
                                     String recommendedDifficulty, List<String> topicLabels) {
    }
}
