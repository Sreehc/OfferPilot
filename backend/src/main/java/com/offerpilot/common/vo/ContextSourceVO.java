package com.offerpilot.common.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContextSourceVO {
    private String type;
    private String label;
    private String summary;
    private String knowledgeScope;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeId;

    private String resumeTitle;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    private String projectName;
}
