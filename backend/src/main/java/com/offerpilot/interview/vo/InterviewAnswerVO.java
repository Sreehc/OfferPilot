package com.offerpilot.interview.vo;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InterviewAnswerVO {
    private BigDecimal score;
    private String comment;
    private String standardAnswer;
    private String followUp;
    private Boolean addedToWrongBook;
    private Boolean hasNextQuestion;
}

