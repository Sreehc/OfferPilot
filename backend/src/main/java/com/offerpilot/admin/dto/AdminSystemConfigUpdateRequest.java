package com.offerpilot.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminSystemConfigUpdateRequest {
    private String configValue;
    @NotNull(message = "enabled is required")
    private Boolean enabled;
    private String changeReason;
}
