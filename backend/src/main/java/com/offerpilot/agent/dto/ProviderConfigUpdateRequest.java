package com.offerpilot.agent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class ProviderConfigUpdateRequest {
    @Valid
    @NotEmpty
    private List<ProviderConfigUpdateItemRequest> configs;
}
