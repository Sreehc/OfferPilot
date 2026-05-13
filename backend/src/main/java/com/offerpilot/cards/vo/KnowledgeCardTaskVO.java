package com.offerpilot.cards.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeCardTaskVO {
    private String id;
    private String docId;
    private String docTitle;
    private String deckTitle;
    private String sourceType;
    private String status;
    private Integer isCurrent;
    private Integer days;
    private Integer currentDay;
    private Integer dailyTarget;
    private Integer totalCards;
    private Integer masteredCards;
    private Integer reviewCount;
    private Integer estimatedMinutes;
    private String invalidReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastStudiedAt;
    private Integer dueCount;
    private Integer reviewedTodayCount;
    private KnowledgeCardItemVO currentCard;
    private List<KnowledgeCardItemVO> cards;
}
