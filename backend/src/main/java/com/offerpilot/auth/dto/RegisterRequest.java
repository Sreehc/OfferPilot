package com.offerpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "cannot be blank")
    @Size(min = 3, max = 64, message = "length must be between 3 and 64")
    private String username;

    @NotBlank(message = "cannot be blank")
    @Size(min = 6, max = 128, message = "length must be between 6 and 128")
    private String password;

    @NotBlank(message = "cannot be blank")
    @Size(max = 64, message = "length must be less than or equal to 64")
    private String nickname;

    @NotBlank(message = "cannot be blank")
    @Email(message = "invalid email")
    @Size(max = 128, message = "length must be less than or equal to 128")
    private String email;

    /** Browser/device fingerprint sent by the frontend */
    private String deviceFingerprint;

    /** Human-readable device name, e.g. "Chrome on Windows" */
    private String deviceName;
}
