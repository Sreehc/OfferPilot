package com.offerpilot.auth.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceVO {
    private Long id;
    private String deviceFingerprint;
    private String deviceName;
    private String ip;
    private String city;
    private LocalDateTime lastActiveTime;
    private LocalDateTime createTime;
    private boolean current;
}
