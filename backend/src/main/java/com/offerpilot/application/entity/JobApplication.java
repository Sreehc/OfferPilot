package com.offerpilot.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("job_application")
@EqualsAndHashCode(callSuper = true)
public class JobApplication extends BaseEntity {
    private Long userId;
    private Long resumeFileId;
    private String company;
    private String jobTitle;
    private String city;
    private String source;
    private String jdText;
    private String status;
    private BigDecimal matchScore;
    private String jdKeywords;
    private String missingKeywords;
    private String analysisSummary;
    private String reviewSuggestion;
    private String nextStepSuggestion;
    private LocalDate applyDate;
    private LocalDate nextStepDate;
}
