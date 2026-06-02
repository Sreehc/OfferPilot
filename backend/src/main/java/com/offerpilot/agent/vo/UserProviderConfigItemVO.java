package com.offerpilot.agent.vo;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProviderConfigItemVO {
    private String scope;
    private String label;
    private String description;
    private Boolean enabled;
    private Boolean configured;
    private String status;
    private String statusMessage;
    private String providerName;
    private String baseUrl;
    private String model;
    private String apiKeyMasked;
    private String accessKeyMasked;
    private String secretKeyMasked;
    private String endpoint;
    private String bucket;
    private String regionName;
    private Integer dimensions;
    private LocalDateTime lastCheckedAt;
    private String lastCheckStatus;
    private String lastCheckMessage;
    private LocalDateTime updateTime;
}
