package com.bytecoach.cards.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardStatsSummaryVO {
    private String currentDeckId;
    private Integer deckCount;
    private Integer totalCards;
    private Integer masteredCards;
    private Integer dueTodayCount;
    private Integer streak;
    private Integer completionRate;
}
