package com.offerpilot.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.dto.WeakPointVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.plan.dto.StudyPlanGenerateRequest;
import com.offerpilot.plan.dto.StudyPlanTaskStatusRequest;
import com.offerpilot.plan.entity.StudyPlan;
import com.offerpilot.plan.entity.StudyPlanTask;
import com.offerpilot.plan.mapper.StudyPlanMapper;
import com.offerpilot.plan.mapper.StudyPlanTaskMapper;
import com.offerpilot.plan.service.PlanService;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private static final Set<Integer> SUPPORTED_DURATIONS = Set.of(7, 14, 30);

    private final StudyPlanMapper studyPlanMapper;
    private final StudyPlanTaskMapper studyPlanTaskMapper;
    private final DashboardService dashboardService;

    @Override
    @Transactional
    public StudyPlanCurrentVO generate(Long userId, StudyPlanGenerateRequest request) {
        validateDuration(request.getDurationDays());
        archiveActivePlans(userId);

        DashboardOverviewVO overview = dashboardService.overview();
        PlanProfile profile = buildProfile(request, overview);

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle(buildPlanTitle(profile));
        plan.setDurationDays(profile.durationDays());
        plan.setFocusDirection(profile.focusDirection());
        plan.setTargetRole(profile.targetRole());
        plan.setTechStack(profile.techStack());
        plan.setWeakPoints(String.join(",", profile.weakPoints()));
        plan.setReviewSuggestion(profile.reviewSuggestion());
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(profile.durationDays() - 1L));
        plan.setCurrentDay(1);
        plan.setProgressRate(BigDecimal.ZERO);
        plan.setCompletedTaskCount(0);
        plan.setDailyTargetMinutes(resolveDailyTargetMinutes(profile.durationDays()));
        studyPlanMapper.insert(plan);

        List<StudyPlanTask> tasks = buildTasks(plan, profile, 1);
        insertTasks(tasks);
        plan.setTotalTaskCount(tasks.size());
        studyPlanMapper.updateById(plan);

        return toCurrentVO(syncPlanState(plan), loadPlanTasks(plan.getId()), overview);
    }

    @Override
    public StudyPlanCurrentVO current(Long userId) {
        StudyPlan plan = studyPlanMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .in(StudyPlan::getStatus, List.of("active", "completed"))
                .orderByDesc(StudyPlan::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            return null;
        }
        StudyPlan synced = syncPlanState(plan);
        return toCurrentVO(synced, loadPlanTasks(synced.getId()), dashboardService.overview());
    }

    @Override
    @Transactional
    public StudyPlanCurrentVO updateTaskStatus(Long userId, Long taskId, StudyPlanTaskStatusRequest request) {
        StudyPlanTask task = studyPlanTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "study plan task not found");
        }

        String status = request.getStatus().toLowerCase(Locale.ROOT);
        if (!List.of("pending", "completed").contains(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "unsupported task status");
        }

        task.setStatus(status);
        task.setCompletedAt("completed".equals(status) ? LocalDateTime.now() : null);
        studyPlanTaskMapper.updateById(task);

        StudyPlan plan = syncPlanState(getOwnedPlan(userId, task.getPlanId()));
        return toCurrentVO(plan, loadPlanTasks(plan.getId()), dashboardService.overview());
    }

    @Override
    @Transactional
    public StudyPlanCurrentVO refresh(Long userId, Long planId) {
        StudyPlan plan = getOwnedPlan(userId, planId);
        if ("archived".equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "archived study plan cannot be refreshed");
        }

        DashboardOverviewVO overview = dashboardService.overview();
        PlanProfile profile = buildProfile(
                new StudyPlanGenerateRequestBuilder(plan.getDurationDays(), plan.getFocusDirection(), plan.getTargetRole(), plan.getTechStack()).build(),
                overview);

        plan = syncPlanState(plan);
        plan.setTitle(buildPlanTitle(profile));
        plan.setFocusDirection(profile.focusDirection());
        plan.setTargetRole(profile.targetRole());
        plan.setTechStack(profile.techStack());
        plan.setWeakPoints(String.join(",", profile.weakPoints()));
        plan.setReviewSuggestion(profile.reviewSuggestion());
        plan.setDailyTargetMinutes(resolveDailyTargetMinutes(profile.durationDays()));
        studyPlanMapper.updateById(plan);

        studyPlanTaskMapper.delete(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, plan.getId())
                .eq(StudyPlanTask::getUserId, userId)
                .ne(StudyPlanTask::getStatus, "completed")
                .ge(StudyPlanTask::getDayIndex, plan.getCurrentDay()));

        insertTasks(buildTasks(plan, profile, plan.getCurrentDay()));
        StudyPlan synced = syncPlanState(getOwnedPlan(userId, plan.getId()));
        return toCurrentVO(synced, loadPlanTasks(synced.getId()), overview);
    }

    private StudyPlan getOwnedPlan(Long userId, Long planId) {
        StudyPlan plan = studyPlanMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "study plan not found");
        }
        return plan;
    }

    private void archiveActivePlans(Long userId) {
        List<StudyPlan> activePlans = studyPlanMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(StudyPlan::getStatus, "active"));
        for (StudyPlan activePlan : activePlans) {
            activePlan.setStatus("archived");
            studyPlanMapper.updateById(activePlan);
        }
    }

    private void validateDuration(Integer durationDays) {
        if (durationDays == null || !SUPPORTED_DURATIONS.contains(durationDays)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "study plan duration must be 7, 14, or 30 days");
        }
    }

    private PlanProfile buildProfile(StudyPlanGenerateRequest request, DashboardOverviewVO overview) {
        List<String> weakPoints = resolveWeakPoints(overview);
        String focusDirection = firstNonBlank(
                request.getFocusDirection(),
                overview.getSuggestedFocus(),
                overview.getRecentInterviews() != null && !overview.getRecentInterviews().isEmpty()
                        ? overview.getRecentInterviews().get(0).getDirection()
                        : null,
                weakPoints.isEmpty() ? null : weakPoints.get(0),
                "Java 后端基础");
        String targetRole = firstNonBlank(request.getTargetRole(), "Java 后端开发");
        String techStack = firstNonBlank(request.getTechStack(), String.join(", ", weakPoints), focusDirection);
        String reviewSuggestion = buildReviewSuggestion(overview, weakPoints, focusDirection);
        return new PlanProfile(
                request.getDurationDays(),
                focusDirection,
                targetRole,
                techStack,
                weakPoints,
                reviewSuggestion,
                overview.getReviewDebtCount() == null ? 0 : overview.getReviewDebtCount(),
                overview.getRecentInterviews() == null ? 0 : overview.getRecentInterviews().size(),
                overview.getApplicationSummary() == null || overview.getApplicationSummary().getActiveCount() == null
                        ? 0
                        : overview.getApplicationSummary().getActiveCount()
        );
    }

    private List<String> resolveWeakPoints(DashboardOverviewVO overview) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (overview.getWeakPoints() != null) {
            for (WeakPointVO item : overview.getWeakPoints()) {
                if (item != null && item.getCategoryName() != null && !item.getCategoryName().isBlank()) {
                    values.add(item.getCategoryName());
                }
                if (values.size() >= 3) {
                    break;
                }
            }
        }
        if (values.size() < 3 && overview.getWeakCategories() != null) {
            for (String category : overview.getWeakCategories()) {
                if (category != null && !category.isBlank()) {
                    values.add(category.trim());
                }
                if (values.size() >= 3) {
                    break;
                }
            }
        }
        return values.isEmpty() ? List.of("高频基础题", "项目表达", "追问深挖") : new ArrayList<>(values);
    }

    private String buildReviewSuggestion(DashboardOverviewVO overview, List<String> weakPoints, String focusDirection) {
        String weakPointText = weakPoints.isEmpty() ? focusDirection : String.join("、", weakPoints);
        int pending = overview.getReviewDebtCount() == null ? 0 : overview.getReviewDebtCount();
        int interviewCount = overview.getRecentInterviews() == null ? 0 : overview.getRecentInterviews().size();
        if (pending > 0) {
            return "先围绕 " + weakPointText + " 清理待复习项，再安排一场 " + focusDirection + " 模拟面试检验表达稳定性。";
        }
        if (interviewCount == 0) {
            return "题库和问答只解决理解问题，计划里要尽快插入第一场模拟面试，把知识点转成可表达答案。";
        }
        return "继续围绕 " + weakPointText + " 做专项推进，并把每轮训练沉淀成下一次面试前的复盘口径。";
    }

    private String buildPlanTitle(PlanProfile profile) {
        return switch (profile.durationDays()) {
            case 7 -> "7 天冲刺计划 | " + profile.focusDirection();
            case 14 -> "14 天强化计划 | " + profile.focusDirection();
            default -> "30 天闭环计划 | " + profile.focusDirection();
        };
    }

    private int resolveDailyTargetMinutes(int durationDays) {
        return switch (durationDays) {
            case 7 -> 75;
            case 14 -> 65;
            default -> 55;
        };
    }

    private List<StudyPlanTask> buildTasks(StudyPlan plan, PlanProfile profile, int startDay) {
        List<StudyPlanTask> tasks = new ArrayList<>();
        boolean heavyReviewLoad = profile.reviewDebtCount() >= 6;
        boolean interviewNeedsCatchUp = profile.recentInterviewCount() == 0 || profile.activeApplicationCount() > 0;
        int interviewInterval = profile.durationDays() <= 7 ? 3 : profile.durationDays() <= 14 ? 4 : 5;
        if (interviewNeedsCatchUp) {
            interviewInterval = Math.max(2, interviewInterval - 1);
        }
        for (int day = startDay; day <= profile.durationDays(); day++) {
            LocalDate taskDate = plan.getStartDate().plusDays(day - 1L);
            tasks.add(buildTask(plan, day, taskDate, "question",
                    "Day " + day + "：题库专项训练",
                    "围绕 " + profile.focusDirection() + " 和 " + joinWeakPoints(profile.weakPoints()) + " 刷一轮结构化题，优先补高频薄弱点。",
                    "/question",
                    35,
                    day <= 3 ? "high" : "medium"));

            if (day % 2 == 1) {
                tasks.add(buildTask(plan, day, taskDate, "chat",
                        "Day " + day + "：RAG 问答补口径",
                        "针对 " + profile.techStack() + " 做学习版或面试版追问，把答案压缩成更稳的表达框架。",
                        "/chat",
                        20,
                        "medium"));
            } else {
                tasks.add(buildTask(plan, day, taskDate, "review",
                        "Day " + day + "：复习与错题回收",
                        heavyReviewLoad
                                ? "先清理积压的待复习项，再把最近低分题整理成下一轮训练前必须回看的清单。"
                                : "清理当前待复习项，并把最近低分题整理成下一轮训练前必须回看的清单。",
                        "/review",
                        20,
                        heavyReviewLoad ? "high" : "medium"));
            }

            if (day % interviewInterval == 0 || day == profile.durationDays()) {
                tasks.add(buildTask(plan, day, taskDate, "interview",
                        "Day " + day + "：模拟面试检验",
                        (interviewNeedsCatchUp ? "优先补一场 " : "安排一场 ")
                                + profile.focusDirection()
                                + " 面试，重点验证 "
                                + profile.targetRole()
                                + " 场景下的回答结构和项目表达。",
                        "/interview",
                        25,
                        "high"));
            }
        }
        return tasks;
    }

    private StudyPlanTask buildTask(StudyPlan plan, int dayIndex, LocalDate taskDate, String module,
                                    String title, String description, String actionPath,
                                    int estimatedMinutes, String priority) {
        StudyPlanTask task = new StudyPlanTask();
        task.setPlanId(plan.getId());
        task.setUserId(plan.getUserId());
        task.setDayIndex(dayIndex);
        task.setTaskDate(taskDate);
        task.setModule(module);
        task.setTitle(title);
        task.setDescription(description);
        task.setActionPath(actionPath);
        task.setEstimatedMinutes(estimatedMinutes);
        task.setPriority(priority);
        task.setStatus("pending");
        return task;
    }

    private void insertTasks(List<StudyPlanTask> tasks) {
        for (StudyPlanTask task : tasks) {
            studyPlanTaskMapper.insert(task);
        }
    }

    private StudyPlan syncPlanState(StudyPlan plan) {
        int currentDay = resolveCurrentDay(plan);
        List<StudyPlanTask> tasks = loadPlanTasks(plan.getId());
        int totalTaskCount = tasks.size();
        int completedTaskCount = (int) tasks.stream().filter(task -> "completed".equals(task.getStatus())).count();
        BigDecimal progressRate = totalTaskCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedTaskCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalTaskCount), 2, RoundingMode.HALF_UP);

        plan.setCurrentDay(currentDay);
        plan.setTotalTaskCount(totalTaskCount);
        plan.setCompletedTaskCount(completedTaskCount);
        plan.setProgressRate(progressRate);
        if (completedTaskCount >= totalTaskCount && totalTaskCount > 0) {
            plan.setStatus("completed");
        } else if (!"archived".equals(plan.getStatus())) {
            plan.setStatus("active");
        }
        studyPlanMapper.updateById(plan);
        return plan;
    }

    private int resolveCurrentDay(StudyPlan plan) {
        long diff = ChronoUnit.DAYS.between(plan.getStartDate(), LocalDate.now()) + 1;
        if (diff <= 1) {
            return 1;
        }
        return (int) Math.min(Math.max(diff, 1), plan.getDurationDays());
    }

    private List<StudyPlanTask> loadPlanTasks(Long planId) {
        return studyPlanTaskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, planId)
                .orderByAsc(StudyPlanTask::getDayIndex)
                .orderByAsc(StudyPlanTask::getId));
    }

    private StudyPlanCurrentVO toCurrentVO(StudyPlan plan, List<StudyPlanTask> tasks, DashboardOverviewVO overview) {
        List<String> weakPoints = splitWeakPoints(plan.getWeakPoints());
        List<StudyPlanTask> todayTasks = tasks.stream()
                .filter(task -> task.getDayIndex() != null && task.getDayIndex().equals(plan.getCurrentDay()))
                .toList();
        List<StudyPlanTask> todayPendingTasks = todayTasks.stream()
                .filter(task -> !"completed".equals(task.getStatus()))
                .toList();
        int todayTaskCount = todayTasks.size();
        return StudyPlanCurrentVO.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .durationDays(plan.getDurationDays())
                .focusDirection(plan.getFocusDirection())
                .targetRole(plan.getTargetRole())
                .techStack(plan.getTechStack())
                .weakPoints(weakPoints)
                .reviewSuggestion(plan.getReviewSuggestion())
                .status(plan.getStatus())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .currentDay(plan.getCurrentDay())
                .progressRate(plan.getProgressRate())
                .totalTaskCount(plan.getTotalTaskCount())
                .completedTaskCount(plan.getCompletedTaskCount())
                .todayTaskCount(todayTaskCount)
                .dailyTargetMinutes(plan.getDailyTargetMinutes())
                .planReasonSummary(buildPlanReasonSummary(plan, overview, weakPoints))
                .todayFocusSummary(buildTodayFocusSummary(plan, todayTasks, todayPendingTasks))
                .trendSummary(buildTrendSummary(plan, todayTasks, todayPendingTasks))
                .tasks(tasks.stream().map(task -> StudyPlanCurrentVO.StudyPlanTaskVO.builder()
                        .id(task.getId())
                        .dayIndex(task.getDayIndex())
                        .taskDate(task.getTaskDate())
                        .module(task.getModule())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .actionPath(task.getActionPath())
                        .estimatedMinutes(task.getEstimatedMinutes())
                        .priority(task.getPriority())
                        .status(task.getStatus())
                        .completedAt(task.getCompletedAt())
                        .build()).toList())
                .build();
    }

    private StudyPlanCurrentVO.PlanReasonSummaryVO buildPlanReasonSummary(
            StudyPlan plan, DashboardOverviewVO overview, List<String> weakPoints) {
        String weakPointText = weakPoints.isEmpty() ? plan.getFocusDirection() : String.join("、", weakPoints);
        int reviewDebtCount = overview.getReviewDebtCount() == null ? 0 : overview.getReviewDebtCount();
        int recentInterviewCount = overview.getRecentInterviews() == null ? 0 : overview.getRecentInterviews().size();
        int activeApplicationCount = overview.getApplicationSummary() == null || overview.getApplicationSummary().getActiveCount() == null
                ? 0
                : overview.getApplicationSummary().getActiveCount();
        List<String> signals = new ArrayList<>();
        signals.add("主攻方向放在 " + plan.getFocusDirection());
        signals.add("当前最该补的是 " + weakPointText);
        if (reviewDebtCount > 0) {
            signals.add("还有 " + reviewDebtCount + " 个待复习项要清掉");
        }
        if (recentInterviewCount == 0) {
            signals.add("还没开始模拟面试，先把表达检验排进去");
        } else {
            signals.add("最近已完成 " + recentInterviewCount + " 场模拟面试");
        }
        if (activeApplicationCount > 0) {
            signals.add("当前有 " + activeApplicationCount + " 个在推进中的投递");
        }
        return StudyPlanCurrentVO.PlanReasonSummaryVO.builder()
                .title("这轮计划为什么先练这些")
                .summary(buildPlanReasonText(plan, weakPointText, reviewDebtCount, recentInterviewCount, activeApplicationCount))
                .signals(signals)
                .build();
    }

    private String buildPlanReasonText(
            StudyPlan plan, String weakPointText, int reviewDebtCount, int recentInterviewCount, int activeApplicationCount) {
        if (reviewDebtCount > 0) {
            return "先围绕 " + weakPointText + " 补薄弱点，再把积压的复习项清掉，能更快把回答稳定下来。";
        }
        if (recentInterviewCount == 0) {
            return "先把 " + plan.getFocusDirection() + " 这条线练顺，再尽快插入模拟面试，避免只会看题不会表达。";
        }
        if (activeApplicationCount > 0) {
            return "当前有真实投递在推进，计划会优先安排能直接服务下一轮面试的训练内容。";
        }
        return "这轮计划会先补 " + weakPointText + "，再用问答和模拟面试把答案练成能直接说出口的表达。";
    }

    private StudyPlanCurrentVO.TodayFocusSummaryVO buildTodayFocusSummary(
            StudyPlan plan, List<StudyPlanTask> todayTasks, List<StudyPlanTask> todayPendingTasks) {
        if (todayTasks.isEmpty()) {
            return StudyPlanCurrentVO.TodayFocusSummaryVO.builder()
                    .state("idle")
                    .title("今天先刷新一下这轮计划")
                    .reason("当前这一天还没有可执行任务，先根据最近训练结果更新安排。")
                    .expectedOutcome("你会拿到一份新的今日清单，知道接下来先练什么。")
                    .build();
        }
        if (todayPendingTasks.isEmpty()) {
            return StudyPlanCurrentVO.TodayFocusSummaryVO.builder()
                    .state("completed")
                    .title("今天的任务已经完成")
                    .reason("这一天的训练已经收口，可以去复盘结果，或者刷新后续安排。")
                    .expectedOutcome("你会更清楚下一轮要继续补哪类题、哪段表达。")
                    .build();
        }

        StudyPlanTask nextTask = todayPendingTasks.get(0);
        int pendingCount = todayPendingTasks.size();
        String weakPointText = joinWeakPoints(splitWeakPoints(plan.getWeakPoints()));
        return StudyPlanCurrentVO.TodayFocusSummaryVO.builder()
                .state("active")
                .title(pendingCount == 1 ? "先完成今天最后 1 项任务" : "先完成今天的 " + pendingCount + " 项任务")
                .reason(buildTodayReason(nextTask, plan.getFocusDirection(), weakPointText))
                .expectedOutcome(buildExpectedOutcome(todayTasks, plan.getTargetRole()))
                .build();
    }

    private String buildTodayReason(StudyPlanTask nextTask, String focusDirection, String weakPointText) {
        return switch (nextTask.getModule()) {
            case "question" -> "先从题库开始，把 " + focusDirection + " 和 " + weakPointText + " 这条线练熟。";
            case "chat" -> "先用问答把知识点压缩成能直接说出口的回答，再进入下一步训练。";
            case "review" -> "先把待复习项清掉，避免旧问题反复拖累今天的训练效果。";
            case "interview" -> "先用一场模拟面试检验今天的准备，确认回答结构能不能真正落地。";
            default -> "先完成当前这项训练，再继续推进后面的安排。";
        };
    }

    private String buildExpectedOutcome(List<StudyPlanTask> todayTasks, String targetRole) {
        boolean includesInterview = todayTasks.stream().anyMatch(task -> "interview".equals(task.getModule()));
        boolean includesChat = todayTasks.stream().anyMatch(task -> "chat".equals(task.getModule()));
        if (includesInterview) {
            return "做完后你会知道这套准备能不能支撑 " + targetRole + " 的真实面试表达。";
        }
        if (includesChat) {
            return "做完后你会把今天的知识点整理成更稳的口头表达，而不只是停留在理解层。";
        }
        return "做完后你会把今天最该补的短板推进一轮，并为下一步训练腾出明确优先级。";
    }

    private StudyPlanCurrentVO.TrendSummaryVO buildTrendSummary(
            StudyPlan plan, List<StudyPlanTask> todayTasks, List<StudyPlanTask> todayPendingTasks) {
        int todayCompletedCount = todayTasks.size() - todayPendingTasks.size();
        int remainingTaskCount = Math.max((plan.getTotalTaskCount() == null ? 0 : plan.getTotalTaskCount())
                - (plan.getCompletedTaskCount() == null ? 0 : plan.getCompletedTaskCount()), 0);
        int remainingDays = Math.max(plan.getDurationDays() - plan.getCurrentDay(), 0);
        String status = todayTasks.isEmpty()
                ? "not_started"
                : todayPendingTasks.isEmpty() ? "on_track" : todayCompletedCount == 0 ? "not_started" : "in_progress";
        String title = switch (status) {
            case "on_track" -> "今天这一步已经跟上节奏";
            case "in_progress" -> "今天已经推进了一部分";
            default -> "今天还没开始，先从第一项任务动起来";
        };
        String summary = switch (status) {
            case "on_track" -> "把后续几天继续按这个节奏执行，计划会稳定收口。";
            case "in_progress" -> "先把剩余任务做完，今天的训练闭环才算真正完成。";
            default -> "先完成第一项高优先任务，页面上的进度和首页摘要会一起更新。";
        };
        return StudyPlanCurrentVO.TrendSummaryVO.builder()
                .status(status)
                .title(title)
                .summary(summary)
                .highlights(List.of(
                        "总进度 " + plan.getCompletedTaskCount() + " / " + plan.getTotalTaskCount(),
                        "今天已完成 " + todayCompletedCount + " / " + todayTasks.size(),
                        "还剩 " + remainingTaskCount + " 项任务，剩余 " + remainingDays + " 天"))
                .build();
    }

    private List<String> splitWeakPoints(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String joinWeakPoints(List<String> weakPoints) {
        return weakPoints.isEmpty() ? "高频基础题" : String.join("、", weakPoints);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private record PlanProfile(
            Integer durationDays,
            String focusDirection,
            String targetRole,
            String techStack,
            List<String> weakPoints,
            String reviewSuggestion,
            Integer reviewDebtCount,
            Integer recentInterviewCount,
            Integer activeApplicationCount) {
    }

    private record StudyPlanGenerateRequestBuilder(
            Integer durationDays,
            String focusDirection,
            String targetRole,
            String techStack) {
        private StudyPlanGenerateRequest build() {
            StudyPlanGenerateRequest request = new StudyPlanGenerateRequest();
            request.setDurationDays(durationDays);
            request.setFocusDirection(focusDirection);
            request.setTargetRole(targetRole);
            request.setTechStack(techStack);
            return request;
        }
    }
}
