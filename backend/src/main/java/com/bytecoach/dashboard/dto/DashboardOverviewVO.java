package com.bytecoach.dashboard.dto;

import com.bytecoach.adaptive.vo.CategoryAbilityVO;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewVO {
    private Integer learningCount;
    private BigDecimal averageScore;
    private Integer wrongCount;
    private Integer planCompletionRate;
    private Integer planHealthScore;
    private List<RecentInterviewVO> recentInterviews;
    private List<WeakPointVO> weakPoints;
    private Boolean firstVisit;

    // Adaptive learning fields
    private Double overallAbility;
    private String recommendedDifficulty;
    private List<String> weakCategories;
    private String suggestedFocus;
    private List<CategoryAbilityVO> categoryAbilities;
}
