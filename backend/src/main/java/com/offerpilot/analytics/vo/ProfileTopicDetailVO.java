package com.offerpilot.analytics.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileTopicDetailVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
    private String categoryName;
    private Double abilityScore;
    private Integer interviewCount;
    private Integer recordingReviewCount;
    private Integer wrongCount;
    private Boolean weak;
    private String recommendedDifficulty;
    private Integer totalCards;
    private Integer masteredCards;
    private Integer dueCount;
    private BigDecimal masteryRate;
    private String summary;
    private List<String> focusRecommendations;
    private List<WeeklyTopicScore> recentScores;

    @Data
    @Builder
    public static class WeeklyTopicScore {
        private String week;
        private Double score;
    }
}
