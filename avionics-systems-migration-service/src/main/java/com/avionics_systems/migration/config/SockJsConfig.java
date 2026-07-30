package com.avionics_systems.migration.config;

import com.avionics_systems.migration.websocket.WebSocketConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class SockJsConfig implements WebSocketConfigurer {

    private final WebSocketConnectionManager webSocketConnectionManager;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Alternative WebSocket handler registration for raw WebSocket connections
        // Note: Primary WebSocket support uses STOMP protocol via WebSocketConfig
        // This provides fallback for specific client requirements
        registry.addHandler(webSocketConnectionManager, "/ws/raw")
                .setAllowedOrigins("*");
    }
}
