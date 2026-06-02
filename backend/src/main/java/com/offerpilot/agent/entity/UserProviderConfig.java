package com.offerpilot.agent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.offerpilot.common.entity.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_provider_config")
@EqualsAndHashCode(callSuper = true)
public class UserProviderConfig extends BaseEntity {
    private Long userId;
    private String providerScope;
    private String providerName;
    private Boolean enabled;
    private String baseUrl;
    private String model;
    private String apiKeyCiphertext;
    private String accessKeyCiphertext;
    private String secretKeyCiphertext;
    private String endpoint;
    private String bucket;
    private String regionName;
    private Integer dimensions;
    private String extraConfigJson;
    private String lastCheckStatus;
    private String lastCheckMessage;
    private LocalDateTime lastCheckedAt;
}
