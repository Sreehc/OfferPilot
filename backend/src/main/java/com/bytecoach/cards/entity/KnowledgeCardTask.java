package com.bytecoach.cards.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("knowledge_card_task")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeCardTask extends BaseEntity {
    private Long userId;
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
}
