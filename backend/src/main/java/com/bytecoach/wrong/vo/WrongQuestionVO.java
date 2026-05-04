package com.bytecoach.wrong.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WrongQuestionVO {
    private Long id;
    private Long questionId;
    private String title;
    private String masteryLevel;
    private String standardAnswer;
    private String errorReason;

    /** SM-2 easiness factor. */
    private BigDecimal easeFactor;

    /** Current interval in days. */
    private Integer intervalDays;

    /** Next scheduled review date. */
    private LocalDate nextReviewDate;

    /** Consecutive successful reviews. */
    private Integer streak;

    /** Total reviews completed. */
    private Integer reviewCount;
}
