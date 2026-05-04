package com.bytecoach.auth.service.impl;

import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;
import com.bytecoach.auth.service.AuthService;
import com.bytecoach.auth.service.DeviceService;
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
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;
    private final DeviceService deviceService;

    @Override
    public LoginResponse register(RegisterRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getNickname());
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        return buildLoginResponse(user, request.getDeviceFingerprint(), request.getDeviceName());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        return buildLoginResponse(user, request.getDeviceFingerprint(), request.getDeviceName());
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
