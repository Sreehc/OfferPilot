package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("interview_record")
@EqualsAndHashCode(callSuper = true)
public class InterviewRecord extends BaseEntity {
    private Long sessionId;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    private BigDecimal score;
    private String comment;
    private String followUp;
    private String scoreDimensionsJson;
    private String weakPointTags;
    private String reviewSummary;
    private Integer isWrong;
}
