package com.offerpilot.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Validates that the JWT secret is not the default value in non-dev profiles.
 * Prevents accidental deployment with an insecure default secret.
 */
@Slf4j
@Component
@Profile("!dev")
public class JwtSecretValidator implements CommandLineRunner {

    private static final String DEFAULT_SECRET = "OfferPilotJwtSecretOfferPilotJwtSecret123456";

    @Value("${offerpilot.security.jwt-secret}")
    private String jwtSecret;

    @Override
    public void run(String... args) {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.equals(DEFAULT_SECRET)) {
            throw new IllegalStateException(
                    "SECURITY: offerpilot.security.jwt-secret is set to the default value or is blank. "
                            + "You MUST configure a strong, unique secret for production deployment. "
                            + "Application startup aborted.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "SECURITY: offerpilot.security.jwt-secret is too short (minimum 32 characters). "
                            + "Application startup aborted.");
        }
        log.info("JWT secret validation passed (length: {})", jwtSecret.length());
    }
}
