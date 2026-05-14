package com.offerpilot.plan.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyPlanCurrentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private Integer durationDays;
    private String focusDirection;
    private String targetRole;
    private String techStack;
    private List<String> weakPoints;
    private String reviewSuggestion;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentDay;
    private BigDecimal progressRate;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private Integer todayTaskCount;
    private Integer dailyTargetMinutes;
    private PlanReasonSummaryVO planReasonSummary;
    private TodayFocusSummaryVO todayFocusSummary;
    private TrendSummaryVO trendSummary;
    private List<StudyPlanTaskVO> tasks;

    @Data
    @Builder
    public static class StudyPlanTaskVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        private Integer dayIndex;
        private LocalDate taskDate;
        private String module;
        private String title;
        private String description;
        private String actionPath;
        private Integer estimatedMinutes;
        private String priority;
        private String status;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    public static class PlanReasonSummaryVO {
        private String title;
        private String summary;
        private List<String> signals;
    }

    @Data
    @Builder
    public static class TodayFocusSummaryVO {
        private String state;
        private String title;
        private String reason;
        private String expectedOutcome;
    }

    @Data
    @Builder
    public static class TrendSummaryVO {
        private String status;
        private String title;
        private String summary;
        private List<String> highlights;
    }
}
