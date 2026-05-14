package com.offerpilot.resume.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeProjectVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String projectName;
    private String roleName;
    private String techStack;
    private String responsibility;
    private String achievement;
    private String projectSummary;
    private List<ResumeProjectQuestionVO> followUpQuestions;
    private List<String> riskHints;
    private Boolean manualEdited;
    private Integer sortOrder;
}
