package com.bytecoach.security.filter;

import com.bytecoach.security.service.DbUserDetailsService;
import com.bytecoach.security.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String DEVICE_TOKEN_PREFIX = "device:token:";

    private final JwtTokenUtil jwtTokenUtil;
    private final DbUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = jwtTokenUtil.resolveToken(request.getHeader("Authorization"));
        if (token != null) {
            if (!jwtTokenUtil.validateToken(token)) {
                log.debug("Invalid or expired JWT token for {} {}", request.getMethod(), request.getRequestURI());
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                if (isTokenBlacklisted(token)) {
                    log.debug("Blacklisted JWT token used for {} {}", request.getMethod(), request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtTokenUtil.parseClaims(token);

                // Check if the device has been revoked
                if (isDeviceRevoked(claims)) {
                    log.debug("Revoked device used for {} {}", request.getMethod(), request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            // Redis unavailable — degrade gracefully (allow the request)
            return false;
        }
    }

    private boolean isDeviceRevoked(Claims claims) {
        try {
            Object userIdObj = claims.get("userId");
            Object dfpObj = claims.get("dfp");
            if (userIdObj == null || dfpObj == null) {
                return false;
            }
            String key = DEVICE_TOKEN_PREFIX + userIdObj + ":" + dfpObj;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            // Redis unavailable — degrade gracefully
            return false;
        }
    }
}
