package com.bytecoach.auth.service;

import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;

public interface AuthService {
    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String authorizationHeader);
}
