package com.offerpilot.resume.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeInterviewResumeVO {
    private String headline;
    private String positioning;
    private String summary;
    private List<String> skillKeywords;
    private List<ProjectHighlightVO> projectHighlights;
    private List<String> speakingChecklist;
    private String exportText;
    private Boolean editable;

    @Data
    @Builder
    public static class ProjectHighlightVO {
        @JsonSerialize(using = ToStringSerializer.class)
        private Long projectId;
        private String projectName;
        private String roleName;
        private String talkingPoint;
        private String result;
    }
}
