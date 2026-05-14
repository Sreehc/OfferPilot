package com.offerpilot.resume.dto;

import java.util.List;
import lombok.Data;

@Data
public class ResumeUpdateRequest {
    private String title;
    private String summary;
    private List<String> skills;
    private String education;
    private String selfIntro;
    private List<ResumeProjectUpdateRequest> projects;

    @Data
    public static class ResumeProjectUpdateRequest {
        private Long id;
        private String projectName;
        private String roleName;
        private String techStack;
        private String responsibility;
        private String achievement;
        private String projectSummary;
        private Integer sortOrder;
    }
}
