package com.offerpilot.interview.websocket;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CopilotWebSocketConfig implements WebSocketConfigurer {

    private final CopilotRealtimeWebSocketHandler realtimeWebSocketHandler;
    private final CopilotWebSocketAuthInterceptor authInterceptor;
    private final List<String> allowedOrigins;

    public CopilotWebSocketConfig(
            CopilotRealtimeWebSocketHandler realtimeWebSocketHandler,
            CopilotWebSocketAuthInterceptor authInterceptor,
            @Value("${offerpilot.cors.origins:http://localhost:5173}") String allowedOrigins) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/interview/copilot/*")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }
}
