package com.offerpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    @NotBlank(message = "tempToken cannot be blank")
    private String tempToken;

    /** 6-digit TOTP code, or recovery code */
    @NotBlank(message = "code cannot be blank")
    private String code;
}
