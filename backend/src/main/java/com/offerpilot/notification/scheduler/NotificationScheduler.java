package com.offerpilot.notification.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.offerpilot.community.entity.UserStats;
import com.offerpilot.community.mapper.UserStatsMapper;
import com.offerpilot.notification.service.NotificationService;
import com.offerpilot.wrong.entity.WrongQuestion;
import com.offerpilot.wrong.mapper.WrongQuestionMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily scheduler that generates reminder notifications for:
 * 1. Review items due today (spaced repetition)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final UserStatsMapper userStatsMapper;
    private final NotificationService notificationService;

    @Scheduled(cron = "${offerpilot.notification.cron-daily-reminder:0 0 8 * * ?}")
    public void dailyReminder() {
        log.info("Starting daily notification reminder job");

        // Get all users who have stats (active users)
        List<UserStats> allStats = userStatsMapper.selectList(null);
        Set<Long> userIds = allStats.stream()
                .map(UserStats::getUserId)
                .collect(Collectors.toSet());

        int reviewNotifications = 0;

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
        }

        log.info("Daily notification reminder job completed: {} review reminders", reviewNotifications);
    }
}
