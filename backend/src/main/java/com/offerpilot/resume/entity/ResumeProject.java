package com.offerpilot.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resume_project")
@EqualsAndHashCode(callSuper = true)
public class ResumeProject extends BaseEntity {
    private Long resumeFileId;
    private Long userId;
    private String projectName;
    private String roleName;
    private String techStack;
    private String responsibility;
    private String achievement;
    private String projectSummary;
    private String followUpQuestionsJson;
    private String riskHints;
    private Integer manualEdited;
    private Integer sortOrder;
}
