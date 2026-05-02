package com.bytecoach.plan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bytecoach.common.entity.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("study_plan")
@EqualsAndHashCode(callSuper = true)
public class StudyPlan extends BaseEntity {
    private Long userId;
    private String title;
    private String goal;
    private String content;
    private Integer days;
    private String status;
    private Integer version;
    private Long parentPlanId;
    private LocalDate startDate;
    private LocalDate endDate;
}
