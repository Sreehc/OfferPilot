package com.bytecoach.analytics.vo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EfficiencyVO {

    /** Current average ease factor across all tracked wrong questions. */
    private BigDecimal avgEaseFactor;

    /** Ease factor trend: list of weekly average EF values. */
    private List<WeeklyEF> efTrend;

    /** Rating distribution: {1: count, 2: count, 3: count, 4: count}. */
    private Map<Integer, Long> ratingDistribution;

    /** Forgetting rate per week (percentage of "Again" ratings). */
    private List<WeeklyForgettingRate> forgettingRateTrend;

    /** Completion rate trend by day or week. */
    private List<CompletionRatePoint> completionRateTrend;

    /** Review debt trend by day or week. */
    private List<DebtTrendPoint> reviewDebtTrend;

    /** Mastered cards growth trend by day or week. */
    private List<MasteredGrowthPoint> masteredGrowthTrend;

    /** Mastery distribution: {not_started: N, reviewing: N, mastered: N}. */
    private Map<String, Long> masteryDistribution;

    /** Review distribution by content type. */
    private Map<String, Long> contentTypeDistribution;

    /** Category mastery summary. */
    private List<CategoryMastery> categoryMastery;

    /** Total reviews completed. */
    private long totalReviews;

    /** Current streak in days. */
    private int currentStreak;

    /** Current review completion rate. */
    private BigDecimal reviewCompletionRate;

    /** Current forgetting rate summary. */
    private BigDecimal forgettingRate;

    @Data
    @Builder
    public static class WeeklyEF {
        private String week;   // e.g. "2026-W18"
        private BigDecimal avgEF;
        private int reviewCount;
    }

    @Data
    @Builder
    public static class WeeklyForgettingRate {
        private String week;
        private double forgettingRate;  // 0.0 - 1.0
        private int totalRatings;
        private int againCount;
    }

    @Data
    @Builder
    public static class CompletionRatePoint {
        private String label;
        private BigDecimal completionRate;
        private int plannedCount;
        private int completedCount;
    }

    @Data
    @Builder
    public static class DebtTrendPoint {
        private String label;
        private int reviewDebtCount;
    }

    @Data
    @Builder
    public static class MasteredGrowthPoint {
        private String label;
        private int masteredCardCount;
    }

    @Data
    @Builder
    public static class CategoryMastery {
        private Long categoryId;
        private String categoryName;
        private int totalCards;
        private int masteredCards;
        private int dueCount;
        private BigDecimal masteryRate;
    }
}
