package com.bytecoach.cards.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("daily_card_task")
@EqualsAndHashCode(callSuper = true)
public class DailyCardTask extends BaseEntity {
    private Long userId;
    private Long taskId;
    private LocalDate taskDate;
    private Integer plannedCount;
    private Integer learnCount;
    private Integer reviewCount;
    private Integer completedCount;
    private Integer streakSnapshot;
    private Integer estimatedMinutes;
    private Integer tomorrowDueCount;
    private String status;
}
