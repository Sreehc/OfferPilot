package com.bytecoach.adaptive.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.adaptive.service.AdaptiveService;
import com.bytecoach.interview.entity.InterviewSession;
import com.bytecoach.interview.mapper.InterviewSessionMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptiveScheduler {

    private final AdaptiveService adaptiveService;
    private final InterviewSessionMapper sessionMapper;

    @Scheduled(cron = "0 10 0 * * ?")
    public void refreshAllProfiles() {
        log.info("Starting daily adaptive profile refresh...");
        try {
            // Get distinct user IDs with finished sessions
            List<Long> userIds = sessionMapper.selectList(
                            new LambdaQueryWrapper<InterviewSession>()
                                    .eq(InterviewSession::getStatus, "finished")
                                    .select(InterviewSession::getUserId))
                    .stream()
                    .map(InterviewSession::getUserId)
                    .distinct()
                    .toList();

            int refreshed = 0;
            for (Long userId : userIds) {
                try {
                    adaptiveService.refreshAbilityProfile(userId);
                    refreshed++;
                } catch (Exception e) {
                    log.warn("Failed to refresh profile for user {}: {}", userId, e.getMessage());
                }
            }
            log.info("Adaptive profile refresh completed: {}/{} users", refreshed, userIds.size());
        } catch (Exception e) {
            log.error("Adaptive profile refresh failed", e);
        }
    }
}
