package com.offerpilot.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("study_plan_task")
@EqualsAndHashCode(callSuper = true)
public class StudyPlanTask extends BaseEntity {
    private Long planId;
    private Long userId;
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
