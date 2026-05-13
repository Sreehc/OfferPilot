package com.offerpilot.wrong.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewStatsVO {
    /** Total reviews completed all time. */
    private int totalReviews;

    /** Consecutive days with at least one review. */
    private int currentStreak;

    /** Today's pending review count. */
    private int todayPending;

    /** Today's completed review count. */
    private int todayCompleted;

    /** Overdue review count. */
    private int overdueCount;

    /** Review count distribution by content type. */
    private Map<String, Integer> contentTypeDistribution;

    /**
     * Review activity heatmap: date string (yyyy-MM-dd) -> review count.
     * Covers the last 90 days.
     */
    private Map<String, Integer> heatmap;
}
