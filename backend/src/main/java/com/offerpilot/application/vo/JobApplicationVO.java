package com.offerpilot.application.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobApplicationVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeFileId;
    private String resumeTitle;
    private String company;
    private String jobTitle;
    private String city;
    private String source;
    private String jdText;
    private String status;
    private BigDecimal matchScore;
    private List<String> jdKeywords;
    private List<String> missingKeywords;
    private String analysisSummary;
    private LocalDate applyDate;
    private LocalDate nextStepDate;
    private LocalDateTime updateTime;
    private List<JobApplicationEventVO> events;
}
