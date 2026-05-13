package com.offerpilot.auth.service;

import com.offerpilot.auth.dto.TwoFactorEnableVO;
import com.offerpilot.auth.dto.TwoFactorSetupVO;
import com.offerpilot.auth.dto.TwoFactorStatusVO;
import com.offerpilot.auth.dto.LoginResponse;

public interface TwoFactorService {

    /**
     * Generate TOTP secret and otpauth URI for setup.
     * Stores the secret temporarily in Redis until enable is called.
     */
    TwoFactorSetupVO setup(Long userId);

    /**
     * Verify the first TOTP code and enable 2FA.
     * Returns recovery codes.
     */
    TwoFactorEnableVO enable(Long userId, String code);

    /**
     * Disable 2FA after verifying current TOTP code.
     */
    void disable(Long userId, String code);

    /**
     * Get 2FA status for the user.
     */
    TwoFactorStatusVO getStatus(Long userId);

    /**
     * Verify TOTP code or recovery code during login.
     * Returns the final LoginResponse with JWT.
     */
    LoginResponse verify(String tempToken, String code);

    /**
     * Generate a temp token for 2FA verification during login.
     * Stored in Redis with short TTL.
     */
    String createTempToken(Long userId, String username, String deviceFingerprint, String deviceName);
}
