package com.offerpilot.analytics.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileTopicRetrospectiveVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;
    private String categoryName;
    private String title;
    private String stage;
    private String summary;
    private List<String> keySignals;
    private List<String> riskSignals;
    private List<String> nextActions;
}
