package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("job_prep_session")
@EqualsAndHashCode(callSuper = true)
public class JobPrepSession extends BaseEntity {
    private Long userId;
    private Long applicationId;
    private Long resumeFileId;
    private String company;
    private String jobTitle;
    private String jdText;
    private String status;
    private BigDecimal matchScore;
    private String matchedKeywordsJson;
    private String missingKeywordsJson;
    private String focusAreasJson;
    private String resumeTalkingPointsJson;
    private String mockQuestionsJson;
    private String nextActionsJson;
    private String summary;
}
