package com.offerpilot.wrong.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTodayItemVO {
    private String reviewItemId;
    private String contentType;
    private String sourceType;
    private String title;
    private String answer;
    private String explanation;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer streak;
    private LocalDate nextReviewDate;
    private LocalDateTime nextReviewAt;
    private long overdueDays;
    private String masteryLevel;

    private String wrongQuestionId;
    private String cardId;
    private String deckId;
    private String deckTitle;
    private String sourceQuote;
}
