package com.offerpilot.interview.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class JobPrepSessionCreateRequest {

    private Long applicationId;

    private Long resumeId;

    private String company;

    private String jobTitle;

    private String jdText;

    @AssertTrue(message = "applicationId or jdText is required")
    public boolean isValid() {
        return applicationId != null || StringUtils.hasText(jdText);
    }
}
