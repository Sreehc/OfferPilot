package com.offerpilot.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuestionUpsertRequest {
    private Long id;

    @NotBlank(message = "cannot be blank")
    private String title;

    @NotNull(message = "cannot be null")
    private Long categoryId;

    @NotBlank(message = "cannot be blank")
    private String difficulty;

    private String type;

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
}
