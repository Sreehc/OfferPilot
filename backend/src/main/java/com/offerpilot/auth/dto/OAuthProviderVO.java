package com.offerpilot.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuthProviderVO {
    private String provider;
    private String displayName;
    private Boolean enabled;
    private Boolean configured;
    private String authUrl;
}
