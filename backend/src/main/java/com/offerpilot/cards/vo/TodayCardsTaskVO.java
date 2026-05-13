package com.offerpilot.cards.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TodayCardsTaskVO {
    private String deckId;
    private String deckTitle;
    private String sourceType;
    private String status;

    private Integer todayLearnCount;
    private Integer todayReviewCount;
    private Integer todayCompletedCount;
    private Integer completionRate;
    private Integer estimatedMinutes;
    private Integer streak;
    private Integer tomorrowDueCount;

    private Integer totalCards;
    private Integer masteredCards;
    private Integer dueCount;
    private Integer reviewedTodayCount;

    private KnowledgeCardItemVO currentCard;
}
