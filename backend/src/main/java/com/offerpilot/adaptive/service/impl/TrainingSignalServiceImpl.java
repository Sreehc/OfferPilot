package com.offerpilot.adaptive.service.impl;

import com.offerpilot.adaptive.service.AdaptiveService;
import com.offerpilot.adaptive.service.TrainingSignalService;
import com.offerpilot.dashboard.service.DashboardService;
import com.offerpilot.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingSignalServiceImpl implements TrainingSignalService {

    private final AdaptiveService adaptiveService;
    private final DashboardService dashboardService;
    private final PlanService planService;

    @Override
    public void handleEvidenceUpdate(Long userId) {
        if (userId == null) {
            return;
        }
        adaptiveService.refreshAbilityProfile(userId);
        dashboardService.evictCache(userId);
        try {
            planService.refreshActivePlan(userId);
        } catch (Exception e) {
            log.warn("Failed to refresh active study plan after evidence update for user {}: {}", userId, e.getMessage());
        }
        dashboardService.evictCache(userId);
    }
}
