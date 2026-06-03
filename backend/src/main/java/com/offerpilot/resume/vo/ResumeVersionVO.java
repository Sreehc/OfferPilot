package com.offerpilot.resume.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResumeVersionVO {
    private Long id;
    private Long resumeFileId;
    private Integer version;
    private String changeSummary;
    private LocalDateTime createTime;
}
