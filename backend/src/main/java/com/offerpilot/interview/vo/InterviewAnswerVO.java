package com.offerpilot.interview.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class InterviewAnswerVO {
    private BigDecimal score;
    private String comment;
    private String standardAnswer;
    private String followUp;
    private List<ScoreDimensionVO> scoreBreakdown;
    private List<String> weakPointTags;
    private String reviewSummary;
    private Boolean addedToWrongBook;
    private Boolean hasNextQuestion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreDimensionVO {
        private String dimension;
        private Integer score;
        private String summary;
    }
}
