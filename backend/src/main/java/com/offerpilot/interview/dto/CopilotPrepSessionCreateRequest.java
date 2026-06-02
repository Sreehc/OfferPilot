package com.offerpilot.interview.dto;

import lombok.Data;

@Data
public class CopilotPrepSessionCreateRequest {
    private Long applicationId;
    private Long resumeId;
    private Long jobPrepSessionId;
    private String company;
    private String jobTitle;
    private String jdText;
    private String notes;
}
