package com.offerpilot.wrong.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("review_log")
public class ReviewLog {
    /**
     * review_log 表没有 update_time 字段，不能复用 BaseEntity 的自动填充映射。
     * 这里显式声明需要落库的字段，避免评分记录插入失败。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
