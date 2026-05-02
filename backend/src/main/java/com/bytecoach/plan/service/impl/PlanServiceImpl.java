package com.bytecoach.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.ai.dto.AiChatRequest;
import com.bytecoach.ai.dto.AiChatResponse;
import com.bytecoach.ai.service.LlmGateway;
import com.bytecoach.ai.service.PromptTemplateService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.plan.dto.GeneratePlanRequest;
import com.bytecoach.plan.dto.PlanTaskStatusRequest;
import com.bytecoach.plan.entity.StudyPlan;
import com.bytecoach.plan.entity.StudyPlanTask;
import com.bytecoach.plan.mapper.StudyPlanMapper;
import com.bytecoach.plan.mapper.StudyPlanTaskMapper;
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
public class PlanServiceImpl implements PlanService {

    private final StudyPlanMapper planMapper;
    private final StudyPlanTaskMapper taskMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final LlmGateway llmGateway;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StudyPlanVO generate(Long userId, GeneratePlanRequest request) {
        int days = request.getDays() != null ? request.getDays() : 7;

        // Collect weak points from wrong questions
        String weakPointsSummary = buildWeakPointsSummary(userId);

        // Deactivate any existing active plans
        deactivateCurrentPlans(userId);

        // Call LLM to generate plan
        String planJson = callLlmForPlan(request.getDirection(), days, weakPointsSummary);
        PlanContent planContent = parsePlanJson(planJson, days);

        // Persist plan
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle(planContent.title());
        plan.setGoal(planContent.goal());
        plan.setContent(planJson);
        plan.setDays(days);
        plan.setStatus("active");
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(days - 1));
        planMapper.insert(plan);

        // Persist tasks
        List<StudyPlanTaskVO> taskVOs = new ArrayList<>();
        for (int i = 0; i < planContent.tasks().size(); i++) {
            PlanTask pt = planContent.tasks().get(i);
            StudyPlanTask task = new StudyPlanTask();
            task.setPlanId(plan.getId());
            task.setUserId(userId);
            task.setTaskDate(LocalDate.now().plusDays(pt.day() - 1));
            task.setTaskType(pt.type());
            task.setContent(pt.content());
            task.setStatus("todo");
            task.setSortOrder(i + 1);
            taskMapper.insert(task);
            taskVOs.add(toTaskVO(task));
        }

        return StudyPlanVO.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .goal(plan.getGoal())
                .status(plan.getStatus())
                .version(plan.getVersion())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .totalTasks(taskVOs.size())
                .completedTasks(0)
                .healthScore(100)
                .tasks(taskVOs)
                .build();
    }

    @Override
    public StudyPlanVO current(Long userId) {
        StudyPlan plan = planMapper.selectOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(StudyPlan::getStatus, "active")
                .orderByDesc(StudyPlan::getCreateTime)
                .last("LIMIT 1"));

        if (plan == null) {
            return null;
        }

        List<StudyPlanTask> taskEntities = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, plan.getId())
                .orderByAsc(StudyPlanTask::getTaskDate, StudyPlanTask::getSortOrder));

        long completedCount = taskEntities.stream().filter(t -> "done".equals(t.getStatus())).count();
        int healthScore = calculateHealthScore(plan.getId());

        return StudyPlanVO.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .goal(plan.getGoal())
                .status(plan.getStatus())
                .version(plan.getVersion())
                .parentPlanId(plan.getParentPlanId())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .totalTasks(taskEntities.size())
                .completedTasks((int) completedCount)
                .healthScore(healthScore)
                .tasks(taskEntities.stream().map(this::toTaskVO).toList())
                .build();
    }

    @Override
    public List<StudyPlanVO> history(Long userId) {
        List<StudyPlan> plans = planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .orderByDesc(StudyPlan::getVersion));

        return plans.stream().map(plan -> {
            List<StudyPlanTask> taskEntities = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                    .eq(StudyPlanTask::getPlanId, plan.getId())
                    .orderByAsc(StudyPlanTask::getTaskDate, StudyPlanTask::getSortOrder));

            long completedCount = taskEntities.stream().filter(t -> "done".equals(t.getStatus())).count();

            return StudyPlanVO.builder()
                    .id(plan.getId())
                    .title(plan.getTitle())
                    .goal(plan.getGoal())
                    .status(plan.getStatus())
                    .version(plan.getVersion())
                    .parentPlanId(plan.getParentPlanId())
                    .startDate(plan.getStartDate())
                    .endDate(plan.getEndDate())
                    .totalTasks(taskEntities.size())
                    .completedTasks((int) completedCount)
                    .healthScore(calculateHealthScore(plan.getId()))
                    .tasks(taskEntities.stream().map(this::toTaskVO).toList())
                    .build();
        }).toList();
    }

    @Override
    public List<StudyPlanTaskVO> tasks(Long userId, Long planId) {
        StudyPlan plan = getOwnedPlan(userId, planId);
        return taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                        .eq(StudyPlanTask::getPlanId, plan.getId())
                        .orderByAsc(StudyPlanTask::getTaskDate, StudyPlanTask::getSortOrder))
                .stream()
                .map(this::toTaskVO)
                .toList();
    }

    @Override
    public void updateTaskStatus(Long userId, Long taskId, PlanTaskStatusRequest request) {
        StudyPlanTask task = taskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "plan task not found");
        }
        task.setStatus(request.getStatus());
        taskMapper.updateById(task);
    }

    // ──────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────

    /**
     * Inline health score calculation (duplicated from PlanAdjustService to avoid circular dependency).
     */
    private int calculateHealthScore(Long planId) {
        List<StudyPlanTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<StudyPlanTask>()
                .eq(StudyPlanTask::getPlanId, planId)
                .orderByAsc(StudyPlanTask::getTaskDate));

        if (tasks.isEmpty()) {
            return 100;
        }

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

        int overallRate = totalDays > 0 ? (int) ((double) completedDays / totalDays * 100) : 100;
        int consecutiveLow = 0;
        LocalDate checkDate = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            List<StudyPlanTask> dayTasks = tasksByDate.get(checkDate);
            if (dayTasks == null || dayTasks.isEmpty()) {
                checkDate = checkDate.minusDays(1);
                continue;
            }
            long done = dayTasks.stream().filter(t -> "done".equals(t.getStatus())).count();
            double rate = (double) done / dayTasks.size();
            if (rate < 0.5) {
                consecutiveLow++;
            } else {
                break;
            }
            checkDate = checkDate.minusDays(1);
        }
        int penalty = consecutiveLow * 10;
        return Math.max(0, Math.min(100, overallRate - penalty));
    }

    private String buildWeakPointsSummary(Long userId) {
        List<WrongQuestion> wrongs = wrongQuestionMapper.selectList(new LambdaQueryWrapper<WrongQuestion>()
                .eq(WrongQuestion::getUserId, userId));

        if (wrongs.isEmpty()) {
            return "No wrong questions yet. Generate a general Java backend study plan.";
        }

        Set<Long> questionIds = wrongs.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        StringBuilder sb = new StringBuilder("Weak points based on wrong questions:\n");
        for (WrongQuestion w : wrongs) {
            Question q = questionMap.get(w.getQuestionId());
            String title = q != null ? q.getTitle() : "Unknown";
            sb.append("- [").append(w.getMasteryLevel()).append("] ").append(title);
            if (w.getErrorReason() != null) {
                sb.append(" — ").append(w.getErrorReason());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private void deactivateCurrentPlans(Long userId) {
        List<StudyPlan> activePlans = planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, userId)
                .eq(StudyPlan::getStatus, "active"));
        for (StudyPlan p : activePlans) {
            p.setStatus("archived");
            planMapper.updateById(p);
        }
    }

    private String callLlmForPlan(String direction, int days, String weakPointsSummary) {
        try {
            String systemPrompt = promptTemplateService.planPrompt();
            String userPrompt = String.format(
                    "Direction: %s\nDays: %d\n\n%s\n\nGenerate a %d-day study plan. Respond with a single JSON object (no markdown):",
                    direction, days, weakPointsSummary, days);
            AiChatResponse response = llmGateway.chatCompletion(AiChatRequest.builder()
                    .systemPrompt(systemPrompt)
                    .userPrompt(userPrompt)
                    .references(List.of())
                    .build());
            return response.getContent();
        } catch (RuntimeException e) {
            log.warn("LLM call failed for plan generation: {}", e.getMessage());
            return "";
        }
    }

    private PlanContent parsePlanJson(String json, int days) {
        if (json == null || json.isBlank()) {
            return fallbackPlan(days);
        }
        try {
            String cleaned = extractJson(json);
            JsonNode root = objectMapper.readTree(cleaned);

            String title = root.has("title") ? root.get("title").asText() : days + "天学习计划";
            String goal = root.has("goal") ? root.get("goal").asText() : "系统复习薄弱知识点";

            List<PlanTask> tasks = new ArrayList<>();
            if (root.has("tasks") && root.get("tasks").isArray()) {
                for (JsonNode taskNode : root.get("tasks")) {
                    int day = taskNode.has("day") ? taskNode.get("day").asInt() : 1;
                    String type = taskNode.has("type") ? taskNode.get("type").asText() : "review";
                    String content = taskNode.has("content") ? taskNode.get("content").asText() : "复习知识点";
                    tasks.add(new PlanTask(day, type, content));
                }
            }
            if (tasks.isEmpty()) {
                return fallbackPlan(days);
            }
            return new PlanContent(title, goal, tasks);
        } catch (Exception e) {
            log.warn("Failed to parse plan JSON: {}", e.getMessage());
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

    private PlanContent fallbackPlan(int days) {
        List<PlanTask> tasks = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            tasks.add(new PlanTask(i, "review", "复习错题本中标记为\"未开始\"和\"复习中\"的知识点"));
            if (i % 2 == 0) {
                tasks.add(new PlanTask(i, "interview", "完成 1 场模拟面试，巩固薄弱方向"));
            }
        }
        return new PlanContent(days + "天冲刺计划", "围绕薄弱点和错题完成复习闭环", tasks);
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

    record PlanTask(int day, String type, String content) {}

    record PlanContent(String title, String goal, List<PlanTask> tasks) {}
}
