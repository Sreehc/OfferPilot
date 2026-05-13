package com.offerpilot.auth.service;

public interface AuthMailService {

    void sendEmailVerificationCode(String email, String code);

    void sendPasswordResetCode(String email, String code);
}
