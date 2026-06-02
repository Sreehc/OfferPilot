package com.offerpilot.analytics.service;

import com.offerpilot.adaptive.vo.AbilityProfileVO;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.analytics.vo.ProfileTopicDetailVO;
import com.offerpilot.analytics.vo.ProfileTopicRetrospectiveVO;
import com.offerpilot.analytics.vo.TrendVO;
import java.util.List;

public interface AnalyticsService {

    /**
     * Ability trend by week, optionally filtered to specific categories.
     * @param weeks number of trailing weeks to include (default 12)
     */
    TrendVO getAbilityTrend(Long userId, int weeks, List<Long> categoryIds);

    /**
     * Review efficiency data: EF trends, forgetting rate, mastery distribution.
     */
    EfficiencyVO getEfficiencyData(Long userId);

    /**
     * Summary insights for dashboard: week-over-week comparison,
     * category changes, and best study hours.
     */
    LearningInsightsVO getLearningInsights(Long userId);

    /**
     * Long-term ability profile used by analytics/profile views.
     */
    AbilityProfileVO getAbilityProfile(Long userId);

    /**
     * Topic-level profile detail used by analytics domain detail drawers.
     */
    ProfileTopicDetailVO getProfileTopicDetail(Long userId, Long categoryId);

    /**
     * Topic retrospective used by analytics retrospective workflow.
     */
    ProfileTopicRetrospectiveVO buildProfileTopicRetrospective(Long userId, Long categoryId);
}
