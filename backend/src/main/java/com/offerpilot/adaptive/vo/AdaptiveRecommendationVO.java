package com.offerpilot.adaptive.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdaptiveRecommendationVO {
    private String id;
    private String type;
    private String title;
    private String reason;
    private String weakPoint;
    private Integer priority;
    private Integer rank;
    private String actionPath;
    private String targetPath;
    private String actionLabel;
    private String tone;
    private List<String> sourceIds;
}
