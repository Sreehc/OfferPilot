package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("copilot_prep_session")
@EqualsAndHashCode(callSuper = true)
public class CopilotPrepSession extends BaseEntity {
    private Long userId;
    private Long applicationId;
    private Long resumeFileId;
    private Long jobPrepSessionId;
    private String company;
    private String jobTitle;
    private String jdText;
    private String notes;
    private String status;
    private String summary;
    private String openingBriefJson;
    private String keyRisksJson;
    private String liveCuesJson;
    private String followUpQuestionsJson;
    private String providerReadinessJson;
    private String nextActionsJson;
}
