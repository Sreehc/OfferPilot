package com.offerpilot.adaptive.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryAbilityVO {
    private Long categoryId;
    private String categoryName;
    private Double abilityScore;
    private Integer interviewCount;
    private Integer recordingReviewCount;
    private Integer jobPrepCount;
    private Integer copilotPrepCount;
    private Integer applicationFeedbackCount;
    private Integer resumeEvidenceCount;
    private Integer wrongCount;
    private Boolean isWeak;
    private String recommendedDifficulty;
}
