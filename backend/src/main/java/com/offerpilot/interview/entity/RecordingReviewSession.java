package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("recording_review_session")
@EqualsAndHashCode(callSuper = true)
public class RecordingReviewSession extends BaseEntity {
    private Long userId;
    private String direction;
    private String jobRole;
    private String notes;
    private String audioUrl;
    private String status;
    private String transcript;
    private BigDecimal transcriptConfidence;
    private Integer transcriptTimeMs;
    private BigDecimal overallScore;
    private String summary;
    private String strengthsJson;
    private String weakPointsJson;
    private String suggestedActionsJson;
}
