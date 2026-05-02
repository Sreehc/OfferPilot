package com.bytecoach.interview.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewDetailVO {
    private Long sessionId;
    private String direction;
    private String status;
    private BigDecimal totalScore;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<InterviewRecordVO> records;

    @Data
    @Builder
    public static class InterviewRecordVO {
        private Long questionId;
        private String questionTitle;
        private String userAnswer;
        private BigDecimal score;
        private String comment;
        private String standardAnswer;
        private String followUp;
    }
}

