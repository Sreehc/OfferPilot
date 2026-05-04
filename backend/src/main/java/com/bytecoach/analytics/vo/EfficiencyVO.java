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

    /** Mastery distribution: {not_started: N, reviewing: N, mastered: N}. */
    private Map<String, Long> masteryDistribution;

    /** Total reviews completed. */
    private long totalReviews;

    /** Current streak in days. */
    private int currentStreak;

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
}
