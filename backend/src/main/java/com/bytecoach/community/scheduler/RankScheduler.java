package com.bytecoach.community.scheduler;

import com.bytecoach.community.service.UserStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankScheduler {

    private final UserStatsService userStatsService;

    @Scheduled(cron = "0 5 0 * * ?")
    public void refreshRanks() {
        log.info("Starting daily rank refresh...");
        try {
            userStatsService.refreshAllRanks();
            log.info("Daily rank refresh completed");
        } catch (Exception e) {
            log.error("Rank refresh failed", e);
        }
    }
}
