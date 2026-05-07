package com.bytecoach.dashboard.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RecentInterviewVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String direction;
    private BigDecimal totalScore;
    private String status;
    private LocalDateTime finishedAt;
}
