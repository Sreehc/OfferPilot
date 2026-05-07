package com.bytecoach.cards.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("knowledge_card")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeCard extends BaseEntity {
    private Long taskId;
    private String question;
    private String answer;
    private Integer sortOrder;
    private Integer scheduledDay;
    private String state;
    private Integer reviewCount;
    private Integer lastRating;
    private LocalDateTime lastReviewTime;
    private BigDecimal easeFactor;
    private Integer intervalDays;
    private Integer streak;
    private LocalDateTime nextReviewAt;
}
