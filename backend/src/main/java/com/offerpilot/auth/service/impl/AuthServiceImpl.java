package com.offerpilot.auth.service.impl;

import com.offerpilot.auth.dto.AuthDeliveryVO;
import com.offerpilot.auth.dto.LoginRequest;
import com.offerpilot.auth.dto.LoginResponse;
import com.offerpilot.auth.dto.OAuthProviderVO;
import com.offerpilot.auth.dto.RegisterRequest;
import com.offerpilot.auth.dto.ResetPasswordRequest;
import com.offerpilot.auth.service.AuthMailService;
import com.offerpilot.auth.service.AuthService;
import com.offerpilot.auth.service.DeviceService;
import com.offerpilot.auth.service.LoginLogService;
import com.offerpilot.auth.service.TwoFactorService;
import com.offerpilot.common.api.ResultCode;
import com.offerpilot.common.config.OfferPilotProperties;
import com.offerpilot.common.exception.BusinessException;
import com.offerpilot.security.model.LoginUser;
import com.offerpilot.security.util.JwtTokenUtil;
import com.offerpilot.user.entity.User;
import com.offerpilot.user.service.UserService;
import com.offerpilot.user.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String EMAIL_VERIFY_PREFIX = "auth:email:verify:";
    private static final String PASSWORD_RESET_PREFIX = "auth:password:reset:";
    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final DeviceService deviceService;
    private final LoginLogService loginLogService;
    private final TwoFactorService twoFactorService;
    private final AuthMailService authMailService;
    private final OfferPilotProperties offerPilotProperties;

    @Override
    public LoginResponse register(RegisterRequest request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getNickname(),
                normalizeEmail(request.getEmail()));
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        return buildLoginResponse(user, request.getDeviceFingerprint(), request.getDeviceName());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Check account lock before attempting authentication
        loginLogService.checkAccountLock(request.getUsername());

        // Verify captcha if provided
        if (request.getCaptchaKey() != null && !request.getCaptchaKey().isBlank()) {
            verifyCaptcha(request.getCaptchaKey(), request.getCaptchaCode());
        }

        String ip = resolveClientIp();
        String device = request.getDeviceName();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            User user = loginUser.getUser();
            user.setLastLoginTime(LocalDateTime.now());
            userService.updateById(user);

            // Record successful login
            try {
                loginLogService.recordLogin(user.getId(), ip, device, true, null);
            } catch (Exception e) {
                log.warn("Failed to record login log: {}", e.getMessage());
            }

            // Check if 2FA is enabled
            if (Boolean.TRUE.equals(user.getTotpEnabled())) {
                String tempToken = twoFactorService.createTempToken(
                        user.getId(), user.getUsername(),
                        request.getDeviceFingerprint(), request.getDeviceName());
                return LoginResponse.builder()
                        .requires2fa(true)
                        .tempToken(tempToken)
                        .build();
            }

            return buildLoginResponse(user, request.getDeviceFingerprint(), request.getDeviceName());
        } catch (AuthenticationException e) {
            // Record failed login
            User user = userService.getByUsername(request.getUsername());
            if (user != null) {
                try {
                    loginLogService.recordLogin(user.getId(), ip, device, false, e.getMessage());
                } catch (Exception ex) {
                    log.warn("Failed to record login log: {}", ex.getMessage());
                }
            }
            throw e;
        }
    }

    private LoginResponse buildLoginResponse(User user, String deviceFingerprint, String deviceName) {
        // Record login device
        Long deviceId = null;
        if (deviceFingerprint != null && !deviceFingerprint.isBlank()) {
            try {
                String ip = resolveClientIp();
                deviceId = deviceService.recordDevice(user.getId(), deviceFingerprint, deviceName, ip, null);
            } catch (Exception e) {
                log.warn("Failed to record login device: {}", e.getMessage());
            }
        }

        return LoginResponse.builder()
                .token(jwtTokenUtil.generateToken(user.getId(), user.getUsername(), deviceId, deviceFingerprint))
                .userInfo(buildUserInfo(user))
                .deviceId(deviceId)
                .build();
    }

    @Override
    public AuthDeliveryVO sendEmailVerificationCode(Long userId) {
        User user = requireUser(userId);
        if (!org.springframework.util.StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请先补充邮箱地址");
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "当前邮箱已验证");
        }

        String code = generateShortCode();
        int ttlMinutes = offerPilotProperties.getAuth().getEmailVerificationCodeTtlMinutes();
        redisTemplate.opsForValue().set(EMAIL_VERIFY_PREFIX + userId, code, ttlMinutes, TimeUnit.MINUTES);
        authMailService.sendEmailVerificationCode(user.getEmail(), code);

        return AuthDeliveryVO.builder()
                .message("验证码已发送，请在设置页完成验证")
                .maskedEmail(maskEmail(user.getEmail()))
                .expiresInMinutes(ttlMinutes)
                .debugCode(exposeDebugCode() ? code : null)
                .build();
    }

    @Override
    public UserInfoVO verifyEmail(Long userId, String code) {
        User user = requireUser(userId);
        String cacheKey = EMAIL_VERIFY_PREFIX + userId;
        String expected = redisTemplate.opsForValue().get(cacheKey);
        if (expected == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码已过期，请重新发送");
        }
        if (!expected.equalsIgnoreCase(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }
        redisTemplate.delete(cacheKey);

        user.setEmailVerified(Boolean.TRUE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userService.updateById(user);
        return buildUserInfo(user);
    }

    @Override
    public AuthDeliveryVO sendPasswordResetCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userService.getByEmail(normalizedEmail);
        int ttlMinutes = offerPilotProperties.getAuth().getPasswordResetCodeTtlMinutes();
        if (user == null) {
            return AuthDeliveryVO.builder()
                    .message("如果邮箱已注册，重置验证码将发送到该邮箱")
                    .maskedEmail(maskEmail(normalizedEmail))
                    .expiresInMinutes(ttlMinutes)
                    .build();
        }

        String code = generateShortCode();
        redisTemplate.opsForValue().set(PASSWORD_RESET_PREFIX + normalizedEmail, code, ttlMinutes, TimeUnit.MINUTES);
        authMailService.sendPasswordResetCode(normalizedEmail, code);
        return AuthDeliveryVO.builder()
                .message("如果邮箱已注册，重置验证码将发送到该邮箱")
                .maskedEmail(maskEmail(normalizedEmail))
                .expiresInMinutes(ttlMinutes)
                .debugCode(exposeDebugCode() ? code : null)
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String cacheKey = PASSWORD_RESET_PREFIX + normalizedEmail;
        String expected = redisTemplate.opsForValue().get(cacheKey);
        if (expected == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码已过期，请重新发送");
        }
        if (!expected.equalsIgnoreCase(request.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }

        User user = userService.getByEmail(normalizedEmail);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "邮箱未注册");
        }
        userService.updatePassword(user.getId(), request.getNewPassword());
        redisTemplate.delete(cacheKey);
    }

    @Override
    public List<OAuthProviderVO> listOAuthProviders() {
        OfferPilotProperties.Auth auth = offerPilotProperties.getAuth();
        boolean githubConfigured = org.springframework.util.StringUtils.hasText(auth.getGithubClientId())
                && org.springframework.util.StringUtils.hasText(auth.getGithubClientSecret());
        return List.of(OAuthProviderVO.builder()
                .provider("github")
                .displayName("GitHub")
                .enabled(auth.isGithubEnabled() && githubConfigured)
                .configured(githubConfigured)
                .authUrl(auth.isGithubEnabled() && githubConfigured ? "/api/auth/oauth/github/authorize" : null)
                .build());
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank()) {
                ip = ip.split(",")[0].trim();
            }
            if (ip == null || ip.isBlank()) {
                ip = req.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank()) {
                ip = req.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    private void verifyCaptcha(String key, String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "请输入验证码");
        }
        String redisKey = CAPTCHA_PREFIX + key;
        String expected = redisTemplate.opsForValue().get(redisKey);
        if (expected == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码已过期，请重新获取");
        }
        redisTemplate.delete(redisKey);
        if (!expected.equalsIgnoreCase(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误");
        }
    }

    @Override
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = jwtTokenUtil.parseClaims(token);
            Date expiration = claims.getExpiration();
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token, "1", ttlMillis, TimeUnit.MILLISECONDS);
                log.info("Token added to blacklist, expires in {} ms", ttlMillis);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "login required");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "user not found");
        }
        return user;
    }

    private UserInfoVO buildUserInfo(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .githubLinked(org.springframework.util.StringUtils.hasText(user.getGithubId()))
                .githubUsername(user.getGithubUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateShortCode() {
        java.util.Random random = new java.util.Random();
        StringBuilder builder = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            builder.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.substring(0, 2) + "***@" + domain;
    }

    private boolean exposeDebugCode() {
        return offerPilotProperties.getAuth().isExposeDebugCodes();
    }
}
