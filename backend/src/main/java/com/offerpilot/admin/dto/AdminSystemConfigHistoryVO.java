package com.offerpilot.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSystemConfigHistoryVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String configGroup;
    private String configKey;
    private String oldValue;
    private String newValue;
    private boolean oldEnabled;
    private boolean newEnabled;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorUserId;
    private String changeReason;
    private LocalDateTime createTime;
}
