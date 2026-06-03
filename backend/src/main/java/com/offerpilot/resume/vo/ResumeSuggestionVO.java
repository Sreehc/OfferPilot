package com.offerpilot.resume.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeSuggestionVO {
    private String field;
    private String severity;
    private String message;
}
