package com.offerpilot.analytics.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningInsightsVO {

    private BigDecimal thisWeekAvgScore;
    private BigDecimal lastWeekAvgScore;
    private int thisWeekInterviewCount;
    private int lastWeekInterviewCount;
    private String todayCompletionStatus;
    private String reviewDebtStatus;
    private String masteryGrowthStatus;
    private String planExecutionStatus;
    private Integer todayPlanCompletedTaskCount;
    private Integer todayPlanTaskCount;
    private BigDecimal activePlanProgressRate;
    private String activePlanTitle;
    private Integer applicationActiveCount;
    private Integer applicationOfferCount;
    private String applicationStatus;
    private Integer resumeCount;
    private String latestResumeTitle;
    private String resumeReadinessStatus;
    private String interviewConversionStatus;
    private String nextActionTitle;
    private String nextActionDescription;
    private String nextActionPath;

    /** Per-category week-over-week changes, sorted by |change| descending. */
    private List<CategoryChange> categoryChanges;

    /** Best study time slots ranked by average score. */
    private List<HourDistribution> bestStudyHours;

    @Data
    @Builder
    public static class CategoryChange {
        private Long categoryId;
        private String categoryName;
        private BigDecimal thisWeekScore;
        private BigDecimal lastWeekScore;
        private BigDecimal change;
    }

    @Data
    @Builder
    public static class HourDistribution {
        private String timeSlot;
        private int sessionCount;
        private BigDecimal avgScore;
    }
}
