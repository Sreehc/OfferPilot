package com.bytecoach.interview.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewHistoryVO {
    private Long sessionId;
    private String direction;
    private String status;
    private BigDecimal totalScore;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
