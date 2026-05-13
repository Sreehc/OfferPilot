package com.offerpilot.cards.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("knowledge_card_log")
public class KnowledgeCardLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long taskId;
    private Long cardId;
    private Integer rating;
    private Integer responseTimeMs;
    private BigDecimal easeFactorBefore;
    private Integer intervalBefore;
    private BigDecimal easeFactorAfter;
    private Integer intervalAfter;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
