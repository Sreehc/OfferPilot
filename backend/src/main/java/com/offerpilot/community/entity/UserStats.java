package com.offerpilot.community.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_stats")
@EqualsAndHashCode(callSuper = true)
public class UserStats extends BaseEntity {
    private Long userId;
    private BigDecimal totalScore;
    private Integer interviewCount;
    private BigDecimal avgScore;
    private Integer reviewStreak;
    private Integer totalReviews;
    private Integer communityScore;
    private Integer communityQuestions;
    private Integer communityAnswers;
    private Integer communityAccepted;
    private String rankTitle;
}
