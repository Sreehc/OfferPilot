package com.offerpilot.interview.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VoiceSubmitVO {
    /** STT transcription result. */
    private String transcript;

    /** STT confidence score. */
    private BigDecimal transcriptConfidence;

    /** STT processing time in ms. */
    private Integer transcriptTimeMs;

    /** AI score (reuses InterviewAnswerVO fields). */
    private BigDecimal score;
    private String comment;
    private String standardAnswer;
    private String followUp;
    private List<InterviewAnswerVO.ScoreDimensionVO> scoreBreakdown;
    private List<String> weakPointTags;
    private String reviewSummary;
    private Boolean addedToWrongBook;
    private Boolean hasNextQuestion;
}
