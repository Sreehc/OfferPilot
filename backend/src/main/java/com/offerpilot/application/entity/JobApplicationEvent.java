package com.offerpilot.application.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("job_application_event")
@EqualsAndHashCode(callSuper = true)
public class JobApplicationEvent extends BaseEntity {
    private Long applicationId;
    private Long userId;
    private String eventType;
    private String title;
    private String content;
    private LocalDateTime eventTime;
    private String result;
}
