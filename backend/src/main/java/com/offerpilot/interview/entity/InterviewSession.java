package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("interview_session")
@EqualsAndHashCode(callSuper = true)
public class InterviewSession extends BaseEntity {
    private Long userId;
    private String direction;
    private String jobRole;
    private String experienceLevel;
    private String techStack;
    private String contextType;
    private Long resumeFileId;
    private Long resumeProjectId;
    private Integer durationMinutes;
    private Integer includeResumeProject;
    private String status;
    private BigDecimal totalScore;
    private Integer questionCount;
    private Integer currentIndex;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** Interview mode: 'text' (default) or 'voice'. */
    private String mode;
}
