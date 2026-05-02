package com.bytecoach.plan.vo;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudyPlanVO {
    private Long id;
    private String title;
    private String goal;
    private String status;
    private Integer version;
    private Long parentPlanId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer healthScore;  // 0-100, based on recent completion rate
    private List<StudyPlanTaskVO> tasks;
}
