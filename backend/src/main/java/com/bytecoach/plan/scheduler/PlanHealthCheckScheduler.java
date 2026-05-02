package com.bytecoach.plan.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bytecoach.plan.entity.StudyPlan;
import com.bytecoach.plan.mapper.StudyPlanMapper;
import com.bytecoach.plan.service.PlanAdjustService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that checks all active plans daily and triggers
 * automatic adjustment for plans with consistently low completion rates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanHealthCheckScheduler {

    private final StudyPlanMapper planMapper;
    private final PlanAdjustService planAdjustService;

    /**
     * Run every day at 23:00 to check plan health.
     * Plans with 3+ consecutive days below 50% completion will be auto-adjusted.
     */
    @Scheduled(cron = "0 0 23 * * ?")
    public void dailyHealthCheck() {
        log.info("Starting daily plan health check...");

        List<StudyPlan> activePlans = planMapper.selectList(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getStatus, "active"));

        int adjusted = 0;
        for (StudyPlan plan : activePlans) {
            try {
                var result = planAdjustService.checkAndAdjust(plan.getUserId());
                if (result != null) {
                    adjusted++;
                    log.info("Auto-adjusted plan for user {}: planId={}", plan.getUserId(), plan.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to check plan health for user {}: {}", plan.getUserId(), e.getMessage());
            }
        }

        log.info("Daily plan health check complete. {} plans adjusted out of {} active plans", adjusted, activePlans.size());
    }
}
