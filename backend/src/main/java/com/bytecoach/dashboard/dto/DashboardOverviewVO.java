package com.bytecoach.dashboard.dto;

import com.bytecoach.adaptive.vo.CategoryAbilityVO;
import com.bytecoach.analytics.vo.LearningInsightsVO;
import com.bytecoach.analytics.vo.EfficiencyVO;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewVO {
    private Integer learningCount;
    private BigDecimal averageScore;
    private Integer wrongCount;
    private List<RecentInterviewVO> recentInterviews;
    private List<WeakPointVO> weakPoints;
    private Boolean firstVisit;

    // Adaptive learning fields
    private Double overallAbility;
    private String recommendedDifficulty;
    private List<String> weakCategories;
    private String suggestedFocus;
    private List<CategoryAbilityVO> categoryAbilities;

    // Memory workflow summary
    private Integer todayLearnCards;
    private Integer todayReviewCards;
    private Integer todayCompletedCards;
    private BigDecimal todayCardCompletionRate;
    private Integer masteredCardCount;
    private Integer reviewDebtCount;
    private Integer studyStreak;
    private String todayCompletionStatus;
    private Integer reviewDebtDelta;
    private List<EfficiencyVO.CategoryMastery> categoryMasterySummary;

    // Analytics insights summary
    private BigDecimal thisWeekAvgScore;
    private BigDecimal lastWeekAvgScore;
    private Integer thisWeekInterviewCount;
    private List<LearningInsightsVO.CategoryChange> categoryChanges;
    private List<LearningInsightsVO.HourDistribution> bestStudyHours;
}
