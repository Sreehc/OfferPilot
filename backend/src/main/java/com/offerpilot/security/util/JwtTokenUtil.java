package com.offerpilot.security.util;

import com.offerpilot.common.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    @Value("${offerpilot.security.jwt-secret}")
    private String jwtSecret;

    @Value("${offerpilot.security.jwt-expire-seconds}")
    private long jwtExpireSeconds;

    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, null, null);
    }

    public String generateToken(Long userId, String username, Long deviceId, String deviceFingerprint) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtExpireSeconds)));
        if (deviceId != null) {
            builder.claim("deviceId", deviceId);
        }
        if (deviceFingerprint != null) {
            builder.claim("dfp", deviceFingerprint);
        }
        return builder.signWith(getSigningKey()).compact();
    }

    public String resolveToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        if (jwtSecret.matches("^[A-Za-z0-9+/=]+$") && jwtSecret.length() >= 44) {
            try {
                keyBytes = Decoders.BASE64.decode(jwtSecret);
            } catch (IllegalArgumentException ex) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        } else {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        Key key = Keys.hmacShaKeyFor(keyBytes);
        return (SecretKey) key;
    }
}

