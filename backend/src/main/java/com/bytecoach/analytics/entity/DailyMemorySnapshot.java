package com.bytecoach.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("daily_memory_snapshot")
@EqualsAndHashCode(callSuper = true)
public class DailyMemorySnapshot extends BaseEntity {
    private Long userId;
    private LocalDate snapshotDate;
    private Integer todayCardTotal;
    private Integer todayCompletedCards;
    private BigDecimal todayCompletionRate;
    private Integer reviewDebtCount;
    private Integer masteredCardCount;
    private Integer dueTodayCount;
    private Integer reviewedTodayCount;
    private Integer studyStreak;
}
