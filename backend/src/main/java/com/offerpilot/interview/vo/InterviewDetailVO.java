package com.offerpilot.interview.vo;

import com.offerpilot.common.vo.ContextSourceVO;
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
    private String contextType;
    private ContextSourceVO contextSource;
    private String status;
    private String mode;
    private BigDecimal totalScore;
    private Boolean isLowScore;
    private String scoreLevel;
    private Integer questionCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<AbilityItemVO> abilityItems;
    private List<WeakRecordVO> weakRecords;
    private List<String> nextTasks;
    private List<InterviewRecordVO> records;

    @Data
    @Builder
    public static class AbilityItemVO {
        private String dimension;
        private BigDecimal score;
        private String summary;
        private Boolean isLowScore;
        private List<String> sourceQuestionIds;
    }

    @Data
    @Builder
    public static class WeakRecordVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long wrongQuestionId;
        private String title;
        private BigDecimal score;
        private String comment;
        private String summary;
        private List<String> tags;
        private Boolean isLowScore;
        private String actionPath;
    }

    @Data
    @Builder
    public static class InterviewRecordVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long questionId;
        @JsonSerialize(using = ToStringSerializer.class)
        private Long wrongQuestionId;
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

        /** Voice-specific: STT transcript (may differ from userAnswer if edited). */
        private String voiceTranscript;
        private BigDecimal voiceConfidence;
    }
}
