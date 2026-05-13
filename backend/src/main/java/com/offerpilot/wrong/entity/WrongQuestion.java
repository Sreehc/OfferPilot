package com.offerpilot.wrong.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("wrong_question")
@EqualsAndHashCode(callSuper = true)
public class WrongQuestion extends BaseEntity {
    private Long userId;
    private Long questionId;
    private String sourceType;
    private String userAnswer;
    private String standardAnswer;
    private String errorReason;
    private String masteryLevel;
    private Integer reviewCount;
    private LocalDateTime lastReviewTime;

    /** SM-2 easiness factor (min 1.30). */
    private BigDecimal easeFactor;

    /** Current review interval in days. */
    private Integer intervalDays;

    /** Next scheduled review date. */
    private LocalDate nextReviewDate;

    /** Consecutive successful reviews (rating >= 3). */
    private Integer streak;
}
