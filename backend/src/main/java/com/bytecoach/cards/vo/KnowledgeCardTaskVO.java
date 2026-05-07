package com.bytecoach.cards.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeCardTaskVO {
    private Long id;
    private Long docId;
    private String docTitle;
    private String status;
    private Integer days;
    private Integer currentDay;
    private Integer dailyTarget;
    private Integer totalCards;
    private Integer masteredCards;
    private Integer reviewCount;
    private String invalidReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer dueCount;
    private Integer reviewedTodayCount;
    private KnowledgeCardItemVO currentCard;
    private List<KnowledgeCardItemVO> cards;
}
