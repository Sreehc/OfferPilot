package com.bytecoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "cannot be blank")
    private String username;

    @NotBlank(message = "cannot be blank")
    private String password;

    /** Browser/device fingerprint sent by the frontend */
    private String deviceFingerprint;

    /** Human-readable device name, e.g. "Chrome on Windows" */
    private String deviceName;
}

