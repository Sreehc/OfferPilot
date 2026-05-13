package com.offerpilot.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("study_plan")
@EqualsAndHashCode(callSuper = true)
public class StudyPlan extends BaseEntity {
    private Long userId;
    private String title;
    private Integer durationDays;
    private String focusDirection;
    private String targetRole;
    private String techStack;
    private String weakPoints;
    private String reviewSuggestion;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentDay;
    private BigDecimal progressRate;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private Integer dailyTargetMinutes;
}
