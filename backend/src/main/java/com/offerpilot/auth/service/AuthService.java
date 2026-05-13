package com.offerpilot.auth.service;

import com.offerpilot.auth.dto.AuthDeliveryVO;
import com.offerpilot.auth.dto.LoginRequest;
import com.offerpilot.auth.dto.LoginResponse;
import com.offerpilot.auth.dto.OAuthProviderVO;
import com.offerpilot.auth.dto.RegisterRequest;
import com.offerpilot.auth.dto.ResetPasswordRequest;
import com.offerpilot.user.vo.UserInfoVO;
import java.util.List;

public interface AuthService {
    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    AuthDeliveryVO sendEmailVerificationCode(Long userId);

    UserInfoVO verifyEmail(Long userId, String code);

    AuthDeliveryVO sendPasswordResetCode(String email);

    void resetPassword(ResetPasswordRequest request);

    List<OAuthProviderVO> listOAuthProviders();

    void logout(String authorizationHeader);
}
