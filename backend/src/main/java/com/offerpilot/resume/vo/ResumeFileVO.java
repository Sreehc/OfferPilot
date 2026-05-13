package com.offerpilot.resume.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeFileVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String fileType;
    private String parseStatus;
    private String summary;
    private List<String> skills;
    private String education;
    private String selfIntro;
    private String interviewResumeText;
    private LocalDateTime lastParsedAt;
    private LocalDateTime updateTime;
    private List<ResumeProjectVO> projects;
}
