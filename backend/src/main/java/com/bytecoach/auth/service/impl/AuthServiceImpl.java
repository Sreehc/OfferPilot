package com.bytecoach.auth.service.impl;

import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;
import com.bytecoach.auth.service.AuthService;
import com.bytecoach.auth.service.DeviceService;
import com.bytecoach.auth.service.LoginLogService;
import com.bytecoach.auth.service.TwoFactorService;
import com.bytecoach.common.api.ResultCode;
import com.bytecoach.common.exception.BusinessException;
import com.bytecoach.security.model.LoginUser;
import com.bytecoach.security.util.JwtTokenUtil;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
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

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final DeviceService deviceService;
    private final LoginLogService loginLogService;
    private final TwoFactorService twoFactorService;

    @Override
    public LoginResponse register(RegisterRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getNickname());
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
        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();

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
                .userInfo(userInfo)
                .deviceId(deviceId)
                .build();
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
}
