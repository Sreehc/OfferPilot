package com.bytecoach.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("interview_session")
@EqualsAndHashCode(callSuper = true)
public class InterviewSession extends BaseEntity {
    private Long userId;
    private String direction;
    private String status;
    private BigDecimal totalScore;
    private Integer questionCount;
    private Integer currentIndex;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** Interview mode: 'text' (default) or 'voice'. */
    private String mode;
}
