package com.offerpilot.adaptive.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbilityProfileVO {
    private Double overallAbility;
    private String recommendedDifficulty;
    private Integer recordingReviewCount;
    private Integer copilotRealtimeCount;
    private List<CategoryAbilityVO> categoryAbilities;
    private List<String> weakCategories;
    private String suggestedFocus;
    private String evidenceStatus;
    private String evidenceSummary;
}
