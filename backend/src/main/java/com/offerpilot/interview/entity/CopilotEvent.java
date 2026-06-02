package com.offerpilot.interview.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("copilot_event")
@EqualsAndHashCode(callSuper = true)
public class CopilotEvent extends BaseEntity {
    private Long sessionId;
    private Long userId;
    private String eventType;
    private String source;
    private String summary;
    private String payloadJson;
}
