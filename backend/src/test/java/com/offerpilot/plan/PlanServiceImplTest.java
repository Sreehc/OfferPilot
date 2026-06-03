package com.offerpilot.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisMapperBuilderAssistant;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.offerpilot.dashboard.dto.DashboardOverviewVO;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.plan.entity.StudyPlan;
import com.offerpilot.plan.entity.StudyPlanTask;
import com.offerpilot.plan.mapper.StudyPlanMapper;
import com.offerpilot.plan.mapper.StudyPlanTaskMapper;
import com.offerpilot.plan.service.impl.PlanServiceImpl;
import com.offerpilot.plan.vo.StudyPlanCurrentVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MybatisMapperBuilderAssistant assistant =
                new MybatisMapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, StudyPlan.class);
        TableInfoHelper.initTableInfo(assistant, StudyPlanTask.class);
    }

    @Mock
    private StudyPlanMapper studyPlanMapper;
    @Mock
    private StudyPlanTaskMapper studyPlanTaskMapper;
    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private PlanServiceImpl planService;

    private final List<StudyPlanTask> storedTasks = new ArrayList<>();
    private StudyPlan activePlan;

    @BeforeEach
    void setUp() {
        storedTasks.clear();

        activePlan = new StudyPlan();
        activePlan.setId(21L);
        activePlan.setUserId(1L);
        activePlan.setTitle("7 天冲刺计划 | Java 后端");
        activePlan.setDurationDays(7);
        activePlan.setFocusDirection("Java 后端");
        activePlan.setTargetRole("后端开发");
        activePlan.setTechStack("Spring Boot, Redis");
        activePlan.setWeakPoints("表达结构,项目案例");
        activePlan.setReviewSuggestion("先补表达，再安排模拟。");
        activePlan.setStatus("active");
        activePlan.setStartDate(LocalDate.now().minusDays(1));
        activePlan.setEndDate(LocalDate.now().plusDays(5));
        activePlan.setCurrentDay(2);
        activePlan.setProgressRate(BigDecimal.ZERO);
        activePlan.setTotalTaskCount(1);
        activePlan.setCompletedTaskCount(0);
        activePlan.setDailyTargetMinutes(75);

        StudyPlanTask todayQuestion = new StudyPlanTask();
        todayQuestion.setId(501L);
        todayQuestion.setPlanId(21L);
        todayQuestion.setUserId(1L);
        todayQuestion.setDayIndex(2);
        todayQuestion.setTaskDate(LocalDate.now());
        todayQuestion.setModule("question");
        todayQuestion.setTitle("Day 2：题库专项训练");
        todayQuestion.setDescription("先练一轮题。");
        todayQuestion.setActionPath("/question");
        todayQuestion.setEstimatedMinutes(35);
        todayQuestion.setPriority("high");
        todayQuestion.setStatus("pending");
        storedTasks.add(todayQuestion);

        when(studyPlanMapper.selectOne(any())).thenReturn(activePlan);
        when(studyPlanMapper.selectById(21L)).thenReturn(activePlan);
        when(studyPlanTaskMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(storedTasks));
        when(studyPlanTaskMapper.selectOne(any())).thenAnswer(invocation -> storedTasks.stream()
                .filter(task -> List.of("recording_review", "topic_retrospective", "interview_review").contains(task.getModule()))
                .findFirst()
                .orElse(null));
        lenient().doAnswer(invocation -> {
            StudyPlanTask task = invocation.getArgument(0);
            if (task.getId() == null) {
                task.setId(900L + storedTasks.size());
            }
            storedTasks.add(task);
            return 1;
        }).when(studyPlanTaskMapper).insert(any(StudyPlanTask.class));
        lenient().doAnswer(invocation -> {
            StudyPlanTask updated = invocation.getArgument(0);
            for (int i = 0; i < storedTasks.size(); i++) {
                if (storedTasks.get(i).getId().equals(updated.getId())) {
                    storedTasks.set(i, updated);
                    break;
                }
            }
            return 1;
        }).when(studyPlanTaskMapper).updateById(any(StudyPlanTask.class));
        when(dashboardService.overview()).thenReturn(DashboardOverviewVO.builder()
                .reviewDebtCount(0)
                .recentInterviews(List.of())
                .weakPoints(List.of())
                .weakCategories(List.of("表达结构"))
                .suggestedFocus("表达结构")
                .build());
    }

    @Test
    void saveRecordingReviewAction_appendsFormalTaskIntoCurrentPlan() {
        StudyPlanCurrentVO result = planService.saveRecordingReviewAction(
                1L,
                55L,
                "Java 后端",
                "后端开发",
                "Spring Boot, Redis",
                "录音复盘专项 | 表达结构",
                "先回听录音片段，再把表达结构带入下一轮模拟。",
                "/interview?workspace=recording-review&recordingReviewSessionId=55");

        assertEquals(2, result.getTodayTaskCount());
        assertTrue(result.getTasks().stream().anyMatch(task ->
                "recording_review".equals(task.getModule())
                        && "录音复盘专项 | 表达结构".equals(task.getTitle())
                        && "/interview?workspace=recording-review&recordingReviewSessionId=55".equals(task.getActionPath())));
    }

    @Test
    void saveRecordingReviewAction_updatesExistingTaskInsteadOfDuplicating() {
        StudyPlanTask existing = new StudyPlanTask();
        existing.setId(777L);
        existing.setPlanId(21L);
        existing.setUserId(1L);
        existing.setDayIndex(2);
        existing.setTaskDate(LocalDate.now().minusDays(1));
        existing.setModule("recording_review");
        existing.setTitle("旧录音复盘任务");
        existing.setDescription("旧描述");
        existing.setActionPath("/interview?workspace=recording-review&recordingReviewSessionId=55");
        existing.setEstimatedMinutes(25);
        existing.setPriority("medium");
        existing.setStatus("pending");
        storedTasks.add(existing);

        StudyPlanCurrentVO result = planService.saveRecordingReviewAction(
                1L,
                55L,
                "Java 后端",
                "后端开发",
                "Spring Boot, Redis",
                "录音复盘专项 | 项目案例支撑",
                "先补项目案例支撑，再回到专项模拟。",
                "/interview?workspace=recording-review&recordingReviewSessionId=55");

        long recordingReviewTaskCount = result.getTasks().stream()
                .filter(task -> "recording_review".equals(task.getModule()))
                .count();
        assertEquals(1, recordingReviewTaskCount);
        assertTrue(result.getTasks().stream().anyMatch(task ->
                "recording_review".equals(task.getModule())
                        && "录音复盘专项 | 项目案例支撑".equals(task.getTitle())
                        && "high".equals(task.getPriority())));
    }

    @Test
    void saveTopicRetrospectiveAction_appendsFormalTaskIntoCurrentPlan() {
        StudyPlanCurrentVO result = planService.saveTopicRetrospectiveAction(
                1L,
                12L,
                "JVM",
                "后端开发",
                "专项题库训练, 定向模拟",
                "领域回顾专项 | JVM",
                "先处理画像分偏低和待复盘点，再补一轮专项题库训练。",
                "/analytics?topic=12");

        assertEquals(2, result.getTodayTaskCount());
        assertTrue(result.getTasks().stream().anyMatch(task ->
                "topic_retrospective".equals(task.getModule())
                        && "领域回顾专项 | JVM".equals(task.getTitle())
                        && "/analytics?topic=12".equals(task.getActionPath())));
    }

    @Test
    void saveInterviewReviewAction_appendsFormalTaskIntoCurrentPlan() {
        StudyPlanCurrentVO result = planService.saveInterviewReviewAction(
                1L,
                77L,
                null,
                "Java 后端",
                "后端开发",
                "Spring, MySQL",
                "面试复盘专项 | Redis",
                "先补 Redis 缓存一致性，再回到一轮专项模拟。",
                "/interview/detail/77");

        assertEquals(2, result.getTodayTaskCount());
        assertTrue(result.getTasks().stream().anyMatch(task ->
                "interview_review".equals(task.getModule())
                        && "面试复盘专项 | Redis".equals(task.getTitle())
                        && "/interview/detail/77".equals(task.getActionPath())));
    }
}
