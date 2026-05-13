package com.offerpilot.interview.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewDetailVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private Integer durationMinutes;
    private Boolean includeResumeProject;
    private String status;
    private String mode;
    private BigDecimal totalScore;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean cardsGenerated;
    private Integer generatedCardCount;
    private String interviewDeckId;
    private String interviewDeckTitle;
    private List<InterviewRecordVO> records;

    @Data
    @Builder
    public static class InterviewRecordVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        private String questionTitle;
        private String userAnswer;
        private BigDecimal score;
        private String comment;
        private String standardAnswer;
        private String followUp;
        private List<InterviewAnswerVO.ScoreDimensionVO> scoreBreakdown;
        private List<String> weakPointTags;
        private String reviewSummary;
        private Boolean isLowScore;
        private String recommendedCardFront;
        private String recommendedCardBack;
        private String recommendedCardExplanation;
        private String recommendedCardFollowUp;
        private String generatedCardId;

        /** Voice-specific: STT transcript (may differ from userAnswer if edited). */
        private String voiceTranscript;
        private BigDecimal voiceConfidence;
    }
}
