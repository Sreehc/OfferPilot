package com.offerpilot.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resume_file")
@EqualsAndHashCode(callSuper = true)
public class ResumeFile extends BaseEntity {
    private Long userId;
    private String title;
    private String fileUrl;
    private String fileType;
    private String parseStatus;
    private String rawText;
    private String summary;
    private String skills;
    private String education;
    private String selfIntro;
    private String interviewResumeText;
    private LocalDateTime lastParsedAt;
}
