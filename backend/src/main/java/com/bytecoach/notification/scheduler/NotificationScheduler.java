package com.bytecoach.notification.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.community.entity.UserStats;
import com.bytecoach.community.mapper.UserStatsMapper;
import com.bytecoach.notification.service.NotificationService;
import com.bytecoach.plan.entity.StudyPlan;
import com.bytecoach.plan.entity.StudyPlanTask;
import com.bytecoach.plan.mapper.StudyPlanMapper;
import com.bytecoach.plan.mapper.StudyPlanTaskMapper;
import com.bytecoach.wrong.entity.WrongQuestion;
import com.bytecoach.wrong.mapper.WrongQuestionMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler that generates reminder notifications for:
 * 1. Review items due today (spaced repetition)
 * 2. Plan tasks due today
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final StudyPlanMapper planMapper;
    private final StudyPlanTaskMapper taskMapper;
    private final UserStatsMapper userStatsMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyReminder() {
        log.info("Starting daily notification reminder job");

        // Get all users who have stats (active users)
        List<UserStats> allStats = userStatsMapper.selectList(null);
        Set<Long> userIds = allStats.stream()
                .map(UserStats::getUserId)
                .collect(Collectors.toSet());

        int reviewNotifications = 0;
        int planNotifications = 0;

        LocalDate today = LocalDate.now();

        for (Long userId : userIds) {
            // 1. Review reminders
            long dueCount = wrongQuestionMapper.selectCount(
                    new LambdaQueryWrapper<WrongQuestion>()
                            .eq(WrongQuestion::getUserId, userId)
                            .le(WrongQuestion::getNextReviewDate, today));

            if (dueCount > 0) {
                try {
                    notificationService.send(userId, "review",
                            "今日复习提醒",
                            "你有 " + dueCount + " 道错题今日需要复习，坚持复习才能巩固记忆！",
                            "/review");
                    reviewNotifications++;
                } catch (Exception e) {
                    log.warn("Failed to send review reminder to user {}: {}", userId, e.getMessage());
                }
            }

            // 2. Plan task reminders
            StudyPlan activePlan = planMapper.selectOne(
                    new LambdaQueryWrapper<StudyPlan>()
                            .eq(StudyPlan::getUserId, userId)
                            .eq(StudyPlan::getStatus, "active")
                            .last("LIMIT 1"));

            if (activePlan != null) {
                long todayTasks = taskMapper.selectCount(
                        new LambdaQueryWrapper<StudyPlanTask>()
                                .eq(StudyPlanTask::getPlanId, activePlan.getId())
                                .eq(StudyPlanTask::getTaskDate, today)
                                .eq(StudyPlanTask::getStatus, "todo"));

                if (todayTasks > 0) {
                    try {
                        notificationService.send(userId, "plan",
                                "学习计划提醒",
                                "你今天有 " + todayTasks + " 个学习任务待完成，保持节奏！",
                                "/plan");
                        planNotifications++;
                    } catch (Exception e) {
                        log.warn("Failed to send plan reminder to user {}: {}", userId, e.getMessage());
                    }
                }
            }
        }

        log.info("Daily notification reminder job completed: {} review reminders, {} plan reminders",
                reviewNotifications, planNotifications);
    }
}
