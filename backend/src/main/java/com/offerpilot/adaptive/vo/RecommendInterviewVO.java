package com.offerpilot.adaptive.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendInterviewVO {
    private String direction;
    private Integer questionCount;
    private String reason;
    private String difficulty;
}
