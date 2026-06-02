package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("copilot_realtime_session")
@EqualsAndHashCode(callSuper = true)
public class CopilotRealtimeSession extends BaseEntity {
    private Long userId;
    private Long copilotPrepSessionId;
    private Long applicationId;
    private Long resumeFileId;
    private Long jobPrepSessionId;
    private String company;
    private String jobTitle;
    private String status;
    private String providerStatus;
    private String prepSummary;
    private String liveChecklistJson;
    private String providerReadinessJson;
    private String latestEventSummary;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private LocalDateTime endedAt;
}
