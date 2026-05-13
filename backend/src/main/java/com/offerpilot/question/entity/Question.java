package com.offerpilot.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("question")
@EqualsAndHashCode(callSuper = true)
public class Question extends BaseEntity {
    private String title;
    private Long categoryId;
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
}
