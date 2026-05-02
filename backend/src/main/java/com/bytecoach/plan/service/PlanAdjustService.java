package com.bytecoach.plan.service;

import com.bytecoach.plan.vo.StudyPlanVO;

public interface PlanAdjustService {

    /**
     * Check if the user's active plan needs adjustment and adjust if necessary.
     * Called by scheduled task or manually by user.
     * @return adjusted plan VO if adjustment was made, null if no adjustment needed
     */
    StudyPlanVO checkAndAdjust(Long userId);

    /**
     * Force adjust a specific plan regardless of completion rate.
     */
    StudyPlanVO forceAdjust(Long userId, Long planId);

    /**
     * Calculate plan health score (0-100) based on recent completion rate.
     */
    int calculateHealthScore(Long planId);
}
