package com.bytecoach.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.LlmGateway;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.plan.entity.StudyPlan;
import com.bytecoach.plan.entity.StudyPlanTask;
import com.bytecoach.plan.mapper.StudyPlanMapper;
import com.bytecoach.plan.mapper.StudyPlanTaskMapper;
import com.bytecoach.plan.service.PlanAdjustService;
import com.bytecoach.plan.service.PlanService;
import com.bytecoach.plan.vo.StudyPlanTaskVO;
import com.bytecoach.plan.vo.StudyPlanVO;
import com.bytecoach.question.entity.Question;
import com.bytecoach.question.mapper.QuestionMapper;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanAdjustServiceImpl implements PlanAdjustService {

    private static final int CONSECUTIVE_LOW_DAYS = 3;
    private static final double LOW_COMPLETION_THRESHOLD = 0.5;

    private final StudyPlanMapper planMapper;
    private final StudyPlanTaskMapper taskMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final LlmGateway llmGateway;
    private final ObjectMapper objectMapper;
    private final PlanService planService;

    @Override
    public StudyPlanVO checkAndAdjust(Long userId) {
        StudyPlan activePlan = planMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(StudyPlan::getStatus, "active")
                .orderByDesc(StudyPlan::getCreateTime)
                .last("LIMIT 1"));

        if (activePlan == null) {
            return null;
        }

        // Check if recent completion rate warrants adjustment
        if (!needsAdjustment(activePlan)) {
            log.debug("Plan {} for user {} does not need adjustment", activePlan.getId(), userId);
            return null;
        }

        log.info("Plan {} for user {} needs adjustment (consecutive low days)", activePlan.getId(), userId);
        return doAdjust(userId, activePlan);
    }

    @Override
    public StudyPlanVO forceAdjust(Long userId, Long planId) {
        StudyPlan plan = getOwnedPlan(userId, planId);
        if (!"active".equals(plan.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能调整进行中的计划");
        }
        return doAdjust(userId, plan);
    }

    @Override
    public int calculateHealthScore(Long planId) {
        List<StudyPlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, planId)
                .orderByAsc(StudyPlanTask::getTaskDate));

        if (tasks.isEmpty()) {
            return 100;
        }

        // Calculate completion rate for each day
        Map<LocalDate, List<StudyPlanTask>> tasksByDate = tasks.stream()
                .collect(Collectors.groupingBy(StudyPlanTask::getTaskDate));

        int totalDays = tasksByDate.size();
        int completedDays = 0;

        for (var entry : tasksByDate.entrySet()) {
            long done = entry.getValue().stream()
                    .filter(t -> "done".equals(t.getStatus()))
                    .count();
            double dayRate = (double) done / entry.getValue().size();
            if (dayRate >= 0.5) {
                completedDays++;
            }
        }

        // Health score: weighted by overall completion + recency
        int overallRate = totalDays > 0 ? (int) ((double) completedDays / totalDays * 100) : 100;

        // Penalize consecutive low days more heavily
        int consecutiveLow = countConsecutiveLowDays(tasksByDate);
        int penalty = consecutiveLow * 10;

        return Math.max(0, Math.min(100, overallRate - penalty));
    }

    @Transactional
    protected StudyPlanVO doAdjust(Long userId, StudyPlan oldPlan) {
        // Build context for LLM
        List<StudyPlanTask> oldTasks = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, oldPlan.getId())
                .orderByAsc(StudyPlanTask::getTaskDate, StudyPlanTask::getSortOrder));

        String taskStatusSummary = buildTaskStatusSummary(oldTasks);
        String weakPointsSummary = buildWeakPointsSummary(userId);
        int remainingDays = calculateRemainingDays(oldPlan);

        // Call LLM to generate adjusted plan
        String planJson = callLlmForAdjustment(oldPlan, taskStatusSummary, weakPointsSummary, remainingDays);

        // Archive old plan
        oldPlan.setStatus("archived");
        planMapper.updateById(oldPlan);

        // Parse and create new plan
        int newVersion = oldPlan.getVersion() != null ? oldPlan.getVersion() + 1 : 2;
        int planDays = Math.max(remainingDays, 3); // At least 3 days

        PlanServiceImpl.PlanContent planContent = parsePlanJson(planJson, planDays);

        StudyPlan newPlan = new StudyPlan();
        newPlan.setUserId(userId);
        newPlan.setTitle(planContent.title());
        newPlan.setGoal(planContent.goal());
        newPlan.setContent(planJson);
        newPlan.setDays(planDays);
        newPlan.setStatus("active");
        newPlan.setVersion(newVersion);
        newPlan.setParentPlanId(oldPlan.getId());
        newPlan.setStartDate(LocalDate.now());
        newPlan.setEndDate(LocalDate.now().plusDays(planDays - 1));
        planMapper.insert(newPlan);

        // Persist new tasks
        List<StudyPlanTaskVO> taskVOs = new ArrayList<>();
        for (int i = 0; i < planContent.tasks().size(); i++) {
            PlanTask pt = planContent.tasks().get(i);
            StudyPlanTask task = new StudyPlanTask();
            task.setPlanId(newPlan.getId());
            task.setUserId(userId);
            task.setTaskDate(LocalDate.now().plusDays(pt.day() - 1));
            task.setTaskType(pt.type());
            task.setContent(pt.content());
            task.setStatus("todo");
            task.setSortOrder(i + 1);
            taskMapper.insert(task);
            taskVOs.add(toTaskVO(task));
        }

        log.info("Adjusted plan for user {}: v{} → v{}, {} tasks", userId, oldPlan.getVersion(), newVersion, taskVOs.size());

        return StudyPlanVO.builder()
                .id(newPlan.getId())
                .title(newPlan.getTitle())
                .goal(newPlan.getGoal())
                .status(newPlan.getStatus())
                .version(newPlan.getVersion())
                .parentPlanId(newPlan.getParentPlanId())
                .startDate(newPlan.getStartDate())
                .endDate(newPlan.getEndDate())
                .totalTasks(taskVOs.size())
                .completedTasks(0)
                .healthScore(100)
                .tasks(taskVOs)
                .build();
    }

    private boolean needsAdjustment(StudyPlan plan) {
        List<StudyPlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, plan.getId())
                .le(StudyPlanTask::getTaskDate, LocalDate.now())
                .orderByDesc(StudyPlanTask::getTaskDate));

        if (tasks.isEmpty()) {
            return false;
        }

        Map<LocalDate, List<StudyPlanTask>> tasksByDate = tasks.stream()
                .collect(Collectors.groupingBy(StudyPlanTask::getTaskDate));

        // Check last N days for consecutive low completion
        int consecutiveLow = 0;
        LocalDate checkDate = LocalDate.now();

        for (int i = 0; i < CONSECUTIVE_LOW_DAYS; i++) {
            List<StudyPlanTask> dayTasks = tasksByDate.get(checkDate);
            if (dayTasks == null || dayTasks.isEmpty()) {
                checkDate = checkDate.minusDays(1);
                continue;
            }

            long done = dayTasks.stream()
                    .filter(t -> "done".equals(t.getStatus()))
                    .count();
            double rate = (double) done / dayTasks.size();

            if (rate < LOW_COMPLETION_THRESHOLD) {
                consecutiveLow++;
            } else {
                break;
            }
            checkDate = checkDate.minusDays(1);
        }

        return consecutiveLow >= CONSECUTIVE_LOW_DAYS;
    }

    private int countConsecutiveLowDays(Map<LocalDate, List<StudyPlanTask>> tasksByDate) {
        int count = 0;
        LocalDate checkDate = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            List<StudyPlanTask> dayTasks = tasksByDate.get(checkDate);
            if (dayTasks == null || dayTasks.isEmpty()) {
                checkDate = checkDate.minusDays(1);
                continue;
            }

            long done = dayTasks.stream()
                    .filter(t -> "done".equals(t.getStatus()))
                    .count();
            double rate = (double) done / dayTasks.size();

            if (rate < LOW_COMPLETION_THRESHOLD) {
                count++;
            } else {
                break;
            }
            checkDate = checkDate.minusDays(1);
        }
        return count;
    }

    private int calculateRemainingDays(StudyPlan plan) {
        if (plan.getEndDate() != null) {
            long remaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), plan.getEndDate());
            return Math.max((int) remaining, 3);
        }
        return plan.getDays() != null ? plan.getDays() : 7;
    }

    private String buildTaskStatusSummary(List<StudyPlanTask> tasks) {
        StringBuilder sb = new StringBuilder();
        Map<String, List<StudyPlanTask>> byStatus = tasks.stream()
                .collect(Collectors.groupingBy(StudyPlanTask::getStatus));

        List<StudyPlanTask> done = byStatus.getOrDefault("done", List.of());
        List<StudyPlanTask> todo = byStatus.getOrDefault("todo", List.of());

        sb.append(String.format("已完成 %d 个任务，未完成 %d 个任务。\n\n", done.size(), todo.size()));

        if (!todo.isEmpty()) {
            sb.append("未完成的任务：\n");
            for (StudyPlanTask t : todo) {
                sb.append(String.format("- [Day %s] %s: %s\n", t.getTaskDate(), t.getTaskType(), t.getContent()));
            }
        }

        return sb.toString();
    }

    private String buildWeakPointsSummary(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId));

        if (wrongs.isEmpty()) {
            return "暂无错题数据。";
        }

        Set<Long> questionIds = wrongs.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        StringBuilder sb = new StringBuilder("当前薄弱点：\n");
        for (WrongQuestion w : wrongs) {
            Question q = questionMap.get(w.getQuestionId());
            String title = q != null ? q.getTitle() : "Unknown";
            sb.append(String.format("- [%s] %s\n", w.getMasteryLevel(), title));
        }
        return sb.toString();
    }

    private String callLlmForAdjustment(StudyPlan oldPlan, String taskStatus, String weakPoints, int remainingDays) {
        try {
            String systemPrompt = planAdjustPrompt();
            String userPrompt = String.format("""
                    原计划标题：%s
                    原计划目标：%s
                    剩余天数：%d

                    %s

                    %s

                    请根据以上信息生成调整后的 %d 天学习计划。未完成的任务优先安排，已掌握的知识点可以减少复习频次。
                    """,
                    oldPlan.getTitle(), oldPlan.getGoal(), remainingDays,
                    taskStatus, weakPoints, remainingDays);

            AiChatResponse response = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(systemPrompt)
                    .userPrompt(userPrompt)
                    .references(List.of())
                    .build());
            return response.getContent();
        } catch (RuntimeException e) {
            log.warn("LLM call failed for plan adjustment: {}", e.getMessage());
            return "";
        }
    }

    private String planAdjustPrompt() {
        return """
                You are ByteCoach, a Java backend study plan optimizer. The user's previous plan had low completion rate.
                Generate an adjusted plan that:
                1. Prioritizes incomplete tasks from the old plan
                2. Reduces volume (fewer tasks per day) to improve completion rate
                3. Focuses on the most impactful weak points
                4. Keeps tasks specific and actionable

                You MUST respond with a single JSON object (no markdown, no extra text) in this exact format:
                {
                  "title": "<adjusted plan title in Chinese>",
                  "goal": "<adjusted goal in Chinese>",
                  "tasks": [
                    {"day": 1, "type": "review", "content": "<task description in Chinese>"},
                    {"day": 1, "type": "interview", "content": "<task description in Chinese>"}
                  ]
                }

                Rules:
                - Each day should have 1-2 tasks (no more than 2)
                - Keep it realistic and achievable
                - All text content must be in Chinese
                """;
    }

    private PlanServiceImpl.PlanContent parsePlanJson(String json, int days) {
        if (json == null || json.isBlank()) {
            return fallbackPlan(days);
        }
        try {
            String cleaned = extractJson(json);
            JsonNode root = objectMapper.readTree(cleaned);

            String title = root.has("title") ? root.get("title").asText() : days + "天调整计划";
            String goal = root.has("goal") ? root.get("goal").asText() : "调整节奏，提升完成率";

            List<PlanServiceImpl.PlanTask> tasks = new ArrayList<>();
            if (root.has("tasks") && root.get("tasks").isArray()) {
                for (JsonNode taskNode : root.get("tasks")) {
                    int day = taskNode.has("day") ? taskNode.get("day").asInt() : 1;
                    String type = taskNode.has("type") ? taskNode.get("type").asText() : "review";
                    String content = taskNode.has("content") ? taskNode.get("content").asText() : "复习知识点";
                    tasks.add(new PlanServiceImpl.PlanTask(day, type, content));
                }
            }
            if (tasks.isEmpty()) {
                return fallbackPlan(days);
            }
            return new PlanServiceImpl.PlanContent(title, goal, tasks);
        } catch (Exception e) {
            log.warn("Failed to parse adjusted plan JSON: {}", e.getMessage());
            return fallbackPlan(days);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private PlanServiceImpl.PlanContent fallbackPlan(int days) {
        List<PlanServiceImpl.PlanTask> tasks = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            tasks.add(new PlanServiceImpl.PlanTask(i, "review", "复习错题本中标记为\"未开始\"和\"复习中\"的知识点（减量版）"));
        }
        return new PlanServiceImpl.PlanContent("调整版学习计划", "降低强度，逐步恢复节奏", tasks);
    }

    private StudyPlan getOwnedPlan(Long userId, Long planId) {
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "study plan not found");
        }
        return plan;
    }

    private StudyPlanTaskVO toTaskVO(StudyPlanTask task) {
        return StudyPlanTaskVO.builder()
                .id(task.getId())
                .taskDate(task.getTaskDate())
                .taskType(task.getTaskType())
                .content(task.getContent())
                .status(task.getStatus())
                .build();
    }

}
