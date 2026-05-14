package com.offerpilot.analytics.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendVO {

    /** Ordered week keys, e.g. ["2026-W15", "2026-W16", ...]. */
    private List<String> weeks;

    /** Memory completion trend by week. */
    private List<MemoryTrendPoint> completionRateTrend;

    /** Review debt trend by week. */
    private List<MemoryTrendPoint> reviewDebtTrend;

    /** Mastered card growth trend by week. */
    private List<MemoryTrendPoint> masteredGrowthTrend;

    /** Overall average interview score per week, kept as supporting context. */
    private List<WeeklyPoint> overallTrend;

    /** Per-category weekly breakdown. */
    private List<CategoryTrend> categoryTrends;

    /** Weekly plan progress snapshots. */
    private List<PlanTrendPoint> planProgressTrend;

    /** Weekly application activity snapshots. */
    private List<ApplicationTrendPoint> applicationActivityTrend;

    /** Weekly resume upload / parse snapshots. */
    private List<ResumeTrendPoint> resumeActivityTrend;

    @Data
    @Builder
    public static class WeeklyPoint {
        private String week;
        private BigDecimal score;
        private int count;
    }

    @Data
    @Builder
    public static class CategoryTrend {
        private Long categoryId;
        private String categoryName;
        private List<WeeklyPoint> points;
    }

    @Data
    @Builder
    public static class MemoryTrendPoint {
        private String week;
        private BigDecimal value;
        private int count;
    }

    @Data
    @Builder
    public static class PlanTrendPoint {
        private String week;
        private BigDecimal progressRate;
        private int completedTaskCount;
        private int totalTaskCount;
    }

    @Data
    @Builder
    public static class ApplicationTrendPoint {
        private String week;
        private int totalCount;
        private int activeCount;
        private int interviewCount;
        private int offerCount;
    }

    @Data
    @Builder
    public static class ResumeTrendPoint {
        private String week;
        private int uploadCount;
        private int parsedCount;
    }
}
