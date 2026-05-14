package com.offerpilot.admin.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSystemConfigVO {
    private String configGroup;
    private String configKey;
    private String displayName;
    private String description;
    private String valueType;
    private String configValue;
    private boolean enabled;
    private String runtimeDefault;
    private List<AdminSystemConfigHistoryVO> configHistory;
}
