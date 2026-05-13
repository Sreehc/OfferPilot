package com.offerpilot.adaptive.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendQuestionsVO {
    private Long questionId;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String difficulty;
    private String reason;
}
