package com.offerpilot.question.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionVO {
    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String type;
    private String difficulty;
    private Integer frequency;
    private String jobDirection;
    private String applicableScope;
    private String tags;
    private String standardAnswer;
    private String interviewAnswer;
    private String followUpSuggestions;
    private String commonMistakes;
    private String scoreStandard;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
