package com.offerpilot.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("agent_run")
@EqualsAndHashCode(callSuper = true)
public class AgentRun extends BaseEntity {
    private Long userId;
    private String agentType;
    private String triggerSource;
    private String status;
    private String title;
    private String summary;
    private String userPrompt;
    private String contextRefsJson;
    private String streamMode;
    private String resultPayloadJson;
    private String nextActionPath;
    private Integer requiresApproval;
}
