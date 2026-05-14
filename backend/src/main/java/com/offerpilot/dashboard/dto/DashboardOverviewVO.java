package com.offerpilot.dashboard.dto;

import com.offerpilot.adaptive.vo.CategoryAbilityVO;
import com.offerpilot.analytics.vo.LearningInsightsVO;
import com.offerpilot.analytics.vo.EfficiencyVO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    private PlanSummary planSummary;
    private ResumeSummary resumeSummary;
    private ApplicationSummary applicationSummary;
    private NextStepSummary nextStep;

    @Data
    @Builder
    public static class PlanSummary {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long planId;
        private String title;
        private Integer currentDay;
        private Integer totalDays;
        private Integer todayTaskCount;
        private Integer completedTaskCount;
        private Integer totalTaskCount;
        private BigDecimal progressRate;
        private String actionPath;
    }

    @Data
    @Builder
    public static class ResumeSummary {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long resumeId;
        private Integer resumeCount;
        private String latestResumeTitle;
        private String parseStatus;
        private Integer projectCount;
        private String actionPath;
    }

    @Data
    @Builder
    public static class ApplicationSummary {
        private Integer totalCount;
        private Integer activeCount;
        private Integer offerCount;
        private BigDecimal averageMatchScore;
        private String topCompany;
        private String actionPath;
    }

    @Data
    @Builder
    public static class NextStepSummary {
        private String title;
        private String description;
        private String actionPath;
    }
}
