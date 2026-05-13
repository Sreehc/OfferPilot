package com.offerpilot.resume.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeProjectQuestionVO {
    private String question;
    private String intent;
}
