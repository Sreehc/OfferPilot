package com.bytecoach.auth.service.impl;

import com.bytecoach.auth.dto.LoginRequest;
import com.bytecoach.auth.dto.LoginResponse;
import com.bytecoach.auth.dto.RegisterRequest;
import com.bytecoach.auth.service.AuthService;
import com.bytecoach.security.model.LoginUser;
import com.bytecoach.security.util.JwtTokenUtil;
import com.bytecoach.user.entity.User;
import com.bytecoach.user.service.UserService;
import com.bytecoach.user.vo.UserInfoVO;
import io.jsonwebtoken.Claims;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginResponse register(RegisterRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getNickname());
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        return buildLoginResponse(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        User user = loginUser.getUser();
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        UserInfoVO userInfo = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
        return LoginResponse.builder()
                .token(jwtTokenUtil.generateToken(user.getId(), user.getUsername()))
                .userInfo(userInfo)
                .build();
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
