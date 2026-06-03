package com.offerpilot.dashboard.dto;

import com.offerpilot.adaptive.vo.CategoryAbilityVO;
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
    private List<RecentInterviewVO> recentInterviews;
    private List<WeakPointVO> weakPoints;
    private Boolean firstVisit;

    private Integer reviewDebtCount;
    private Integer studyStreak;

    private List<String> weakCategories;
    private String suggestedFocus;
    private List<CategoryAbilityVO> categoryAbilities;

    private NextActionVO nextAction;
    private ApplicationSummary applicationSummary;
    private List<WorkflowContinuation> workflowContinuations;

    @Data
    @Builder
    public static class ApplicationSummary {
        private Integer totalCount;
        private Integer activeCount;
        private Integer offerCount;
        private BigDecimal averageMatchScore;
        private String topCompany;
        private String actionPath;
    }

    @Data
    @Builder
    public static class WorkflowContinuation {
        private String key;
        private String label;
        private String status;
        private String description;
        private String path;
        private String tone;
    }
}
