package com.offerpilot.application.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobApplicationEventVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String eventType;
    private String title;
    private String content;
    private LocalDateTime eventTime;
    private String result;
    private Integer interviewRound;
    private String interviewer;
    private java.util.List<String> feedbackTags;
}
