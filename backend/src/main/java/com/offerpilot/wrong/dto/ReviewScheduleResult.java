package com.offerpilot.wrong.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewScheduleResult {
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer streak;
    private LocalDate nextReviewDate;
    private String masteryLevel;
}
