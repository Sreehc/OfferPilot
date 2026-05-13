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

        return toCurrentVO(syncPlanState(plan), loadPlanTasks(plan.getId()));
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
        return toCurrentVO(synced, loadPlanTasks(synced.getId()));
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
        return toCurrentVO(plan, loadPlanTasks(plan.getId()));
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
        return toCurrentVO(synced, loadPlanTasks(synced.getId()));
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
                reviewSuggestion
        );
    }

    private List<String> resolveWeakPoints(DashboardOverviewVO overview) {
        if (overview.getWeakPoints() == null || overview.getWeakPoints().isEmpty()) {
            return List.of("高频基础题", "项目表达", "追问深挖");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (WeakPointVO item : overview.getWeakPoints()) {
            if (item != null && item.getCategoryName() != null && !item.getCategoryName().isBlank()) {
                values.add(item.getCategoryName());
            }
            if (values.size() >= 3) {
                break;
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
        int interviewInterval = profile.durationDays() <= 7 ? 3 : profile.durationDays() <= 14 ? 4 : 5;
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
                        "清理当前待复习项，并把最近低分题整理成下一轮训练前必须回看的清单。",
                        "/review",
                        20,
                        "medium"));
            }

            if (day % interviewInterval == 0 || day == profile.durationDays()) {
                tasks.add(buildTask(plan, day, taskDate, "interview",
                        "Day " + day + "：模拟面试检验",
                        "安排一场 " + profile.focusDirection() + " 面试，重点验证 " + profile.targetRole() + " 场景下的回答结构和项目表达。",
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

    private StudyPlanCurrentVO toCurrentVO(StudyPlan plan, List<StudyPlanTask> tasks) {
        List<String> weakPoints = splitWeakPoints(plan.getWeakPoints());
        int todayTaskCount = (int) tasks.stream()
                .filter(task -> task.getDayIndex() != null && task.getDayIndex().equals(plan.getCurrentDay()))
                .count();
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
            String reviewSuggestion) {
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
