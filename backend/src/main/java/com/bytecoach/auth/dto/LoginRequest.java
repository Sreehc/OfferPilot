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

    /** Captcha key (required after consecutive failures) */
    private String captchaKey;

    /** Captcha code entered by user */
    private String captchaCode;
}
