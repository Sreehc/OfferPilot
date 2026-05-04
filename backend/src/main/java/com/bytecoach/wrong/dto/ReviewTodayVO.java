package com.bytecoach.wrong.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTodayVO {
    private Long wrongQuestionId;
    private Long questionId;
    private String title;
    private String standardAnswer;
    private String errorReason;

    /** SM-2 easiness factor. */
    private BigDecimal easeFactor;

    /** Current interval in days. */
    private Integer intervalDays;

    /** How many consecutive successful reviews so far. */
    private Integer streak;

    /** Scheduled review date (may be in the past if overdue). */
    private LocalDate nextReviewDate;

    /** Days overdue (0 if due today, negative if future). */
    private long overdueDays;

    /** Computed mastery level based on easeFactor and streak. */
    private String masteryLevel;
}
