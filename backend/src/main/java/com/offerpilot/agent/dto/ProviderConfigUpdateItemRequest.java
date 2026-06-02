package com.offerpilot.agent.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderConfigUpdateItemRequest {
    @NotBlank
    private String scope;
    private Boolean enabled;
    private String providerName;
    private String baseUrl;
    private String model;
    private String apiKey;
    private Boolean clearApiKey;
    private String accessKey;
    private Boolean clearAccessKey;
    private String secretKey;
    private Boolean clearSecretKey;
    private String endpoint;
    private String bucket;
    private String regionName;
    private Integer dimensions;
}
