package com.bytecoach.analytics.vo;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrendVO {

    /** Ordered week keys, e.g. ["2026-W15", "2026-W16", ...]. */
    private List<String> weeks;

    /** Overall average score per week. */
    private List<WeeklyPoint> overallTrend;

    /** Per-category weekly breakdown. */
    private List<CategoryTrend> categoryTrends;

    @Data
    @Builder
    public static class WeeklyPoint {
        private String week;
        private BigDecimal score;
        private int count;
    }

    @Data
    @Builder
    public static class CategoryTrend {
        private Long categoryId;
        private String categoryName;
        private List<WeeklyPoint> points;
    }
}
