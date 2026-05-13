package com.offerpilot.wrong.support;

import com.offerpilot.wrong.dto.ReviewScheduleResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class ReviewSchedulingRules {

    public static final BigDecimal MIN_EASE_FACTOR = new BigDecimal("1.30");
    public static final BigDecimal DEFAULT_EASE_FACTOR = new BigDecimal("2.50");

    private ReviewSchedulingRules() {
    }

    public static ReviewScheduleResult schedule(BigDecimal easeFactor, Integer intervalDays, Integer streak, Integer rating) {
        BigDecimal efBefore = easeFactor != null ? easeFactor : DEFAULT_EASE_FACTOR;
        int previousInterval = intervalDays != null ? intervalDays : 0;
        int previousStreak = streak != null ? streak : 0;

        BigDecimal efAfter;
        int intervalAfter;
        int newStreak;

        if (rating < 3) {
            intervalAfter = 1;
            newStreak = 0;
            BigDecimal penalty = rating == 1 ? new BigDecimal("0.20") : new BigDecimal("0.15");
            efAfter = efBefore.subtract(penalty).max(MIN_EASE_FACTOR);
        } else {
            newStreak = previousStreak + 1;
            if (newStreak == 1) {
                intervalAfter = 1;
            } else if (newStreak == 2) {
                intervalAfter = 6;
            } else {
                int baseInterval = Math.max(1, previousInterval);
                intervalAfter = BigDecimal.valueOf(baseInterval)
                        .multiply(efBefore)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
            }

            int quality = rating == 4 ? 5 : 3;
            BigDecimal efDelta = new BigDecimal("0.1")
                    .subtract(BigDecimal.valueOf(5 - quality)
                            .multiply(new BigDecimal("0.08")
                                    .add(BigDecimal.valueOf(5 - quality)
                                            .multiply(new BigDecimal("0.02")))));
            efAfter = efBefore.add(efDelta).max(MIN_EASE_FACTOR);
        }

        return ReviewScheduleResult.builder()
                .easeFactor(efAfter)
                .intervalDays(intervalAfter)
                .streak(newStreak)
                .nextReviewDate(LocalDate.now().plusDays(intervalAfter))
                .masteryLevel(resolveMasteryLevel(efAfter, newStreak))
                .build();
    }

    public static String resolveMasteryLevel(BigDecimal easeFactor, Integer streak) {
        BigDecimal ef = easeFactor != null ? easeFactor : DEFAULT_EASE_FACTOR;
        int s = streak != null ? streak : 0;

        if (ef.compareTo(new BigDecimal("2.3")) >= 0 && s >= 3) {
            return "mastered";
        }
        if (ef.compareTo(new BigDecimal("1.8")) >= 0 || s >= 1) {
            return "reviewing";
        }
        return "not_started";
    }
}
