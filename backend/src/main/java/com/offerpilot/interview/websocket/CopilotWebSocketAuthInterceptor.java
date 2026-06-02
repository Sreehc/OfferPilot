package com.offerpilot.interview.websocket;

import com.offerpilot.security.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class CopilotWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = firstNonBlank(params.getFirst("token"), params.getFirst("access_token"));
        if (!StringUtils.hasText(token) || !jwtTokenUtil.validateToken(token)) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return false;
        }
        Claims claims = jwtTokenUtil.parseClaims(token);
        Object userId = claims.get("userId");
        if (userId == null) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("userId", Long.valueOf(String.valueOf(userId)));
        attributes.put("username", claims.getSubject());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private void reject(ServerHttpResponse response, HttpStatus status) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            servletResponse.getServletResponse().setStatus(status.value());
        } else {
            response.setStatusCode(status);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
