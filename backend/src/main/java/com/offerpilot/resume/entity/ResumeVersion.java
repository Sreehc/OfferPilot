package com.offerpilot.resume.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("resume_version")
@EqualsAndHashCode(callSuper = true)
public class ResumeVersion extends BaseEntity {
    private Long resumeFileId;
    private Long userId;
    private Integer version;
    private String snapshotJson;
    private String changeSummary;
}
