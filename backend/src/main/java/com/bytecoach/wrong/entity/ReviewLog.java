package com.bytecoach.wrong.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("review_log")
@EqualsAndHashCode(callSuper = true)
public class ReviewLog extends BaseEntity {
    private Long userId;
    private Long wrongQuestionId;
    /** User rating 1-4 (1=Again, 2=Hard, 3=Good, 4=Easy). */
    private Integer rating;
    /** Time spent reviewing in ms. */
    private Integer responseTimeMs;
    private BigDecimal easeFactorBefore;
    private Integer intervalBefore;
    private BigDecimal easeFactorAfter;
    private Integer intervalAfter;
}
