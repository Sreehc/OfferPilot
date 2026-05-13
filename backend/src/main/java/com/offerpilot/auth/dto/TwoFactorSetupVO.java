package com.offerpilot.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSetupVO {
    /** otpauth:// URI for QR code generation */
    private String otpauthUri;
    /** Base64 encoded QR code image (optional, frontend can generate from URI) */
    private String qrCodeImage;
    /** TOTP secret (for manual entry) */
    private String secret;
}
