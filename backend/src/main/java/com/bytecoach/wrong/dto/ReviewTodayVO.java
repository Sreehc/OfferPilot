package com.bytecoach.wrong.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTodayVO {
    private String selectedContentType;
    private Integer totalPending;
    private Integer overdueCount;
    private Integer todayCompleted;
    private Integer currentStreak;
    private Map<String, Integer> countsByContentType;
    private List<ReviewTodayItemVO> items;
}
