package com.bytecoach.cards.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDeckSummaryVO {
    private String deckId;
    private String deckTitle;
    private String sourceType;
    private String status;
    private Integer totalCards;
    private Integer masteredCards;
    private Integer dueCount;
    private Integer reviewedTodayCount;
    private Integer isCurrent;
    private LocalDateTime lastStudiedAt;
}
