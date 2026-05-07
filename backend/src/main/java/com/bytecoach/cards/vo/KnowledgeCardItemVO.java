package com.bytecoach.cards.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeCardItemVO {
    private String id;
    private String question;
    private String answer;
    private Integer sortOrder;
    private Integer scheduledDay;
    private String state;
    private Integer reviewCount;
    private Integer lastRating;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer streak;
}
