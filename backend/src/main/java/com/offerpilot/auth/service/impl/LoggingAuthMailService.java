package com.offerpilot.auth.service.impl;

import com.offerpilot.auth.service.AuthMailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingAuthMailService implements AuthMailService {

    @Override
    public void sendEmailVerificationCode(String email, String code) {
        log.info("Email verification code generated for {}: {}", email, code);
    }

    @Override
    public void sendPasswordResetCode(String email, String code) {
        log.info("Password reset code generated for {}: {}", email, code);
    }
}
