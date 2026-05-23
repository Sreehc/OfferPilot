package com.offerpilot.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NextActionVO {
    private String key;
    private String title;
    private String description;
    private String path;
    private String reason;
    private String priority;
}
