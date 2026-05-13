package com.offerpilot.question.dto;

import com.offerpilot.common.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionQuery extends PageQuery {
    private Long categoryId;
    private String difficulty;
    private String keyword;
}

