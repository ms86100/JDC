package com.jira.migration.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketSession;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class WebSocketSecurityConfig implements WebSocketConfigurer {

    @Value("${websocket.max-connections-per-user:10}")
    private int maxConnectionsPerUser;

    @Value("${websocket.rate-limit-per-second:100}")
    private int rateLimitPerSecond;

    // Track active connections per user
    private final Map<String, AtomicInteger> userConnectionCounts = new ConcurrentHashMap<>();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Security configuration is handled through interceptors in WebSocketConfig
        // This class provides connection limiting and rate limiting configuration
    }

    public boolean canAcceptConnection(String userId) {
        AtomicInteger count = userConnectionCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        if (count.get() >= maxConnectionsPerUser) {
            log.warn("User {} rejected: too many connections ({}), max allowed: {}",
                    userId, count.get(), maxConnectionsPerUser);
            return false;
        }
        count.incrementAndGet();
        return true;
    }

    public void releaseConnection(String userId) {
        AtomicInteger count = userConnectionCounts.get(userId);
        if (count != null) {
            int remaining = count.decrementAndGet();
            if (remaining <= 0) {
                userConnectionCounts.remove(userId);
            }
        }
    }

    public int getActiveConnectionCount(String userId) {
        AtomicInteger count = userConnectionCounts.get(userId);
        return count != null ? count.get() : 0;
    }

    public Map<String, Integer> getAllActiveConnections() {
        return Map.copyOf(userConnectionCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                )));
    }

    public String getOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = request.getHeader("Referer");
        }
        if (origin == null) {
            origin = "unknown";
        }
        return origin;
    }

    public boolean isOriginAllowed(String origin) {
        // For development, allow all origins
        // In production, this should validate against configured allowed origins
        if ("*".equals(allowedOrigins)) {
            return true;
        }
        return allowedOrigins.contains(origin);
    }

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;
}
