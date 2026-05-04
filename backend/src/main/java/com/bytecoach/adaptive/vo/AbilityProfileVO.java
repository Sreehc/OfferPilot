package com.bytecoach.adaptive.vo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbilityProfileVO {
    private Double overallAbility;
    private String recommendedDifficulty;
    private List<CategoryAbilityVO> categoryAbilities;
    private List<String> weakCategories;
    private String suggestedFocus;
}
