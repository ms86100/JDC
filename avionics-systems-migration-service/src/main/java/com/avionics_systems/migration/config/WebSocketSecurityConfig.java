package com.avionics_systems.migration.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket security configuration with connection limiting and rate limiting.
 * Provides per-user connection limits and message rate limiting.
 */
@Configuration
@Slf4j
public class WebSocketSecurityConfig implements WebSocketConfigurer {

    @Value("${websocket.max-connections-per-user:10}")
    private int maxConnectionsPerUser;

    @Value("${websocket.rate-limit-per-second:100}")
    private int rateLimitPerSecond;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    // Track active connections per user
    private final Map<String, AtomicInteger> userConnectionCounts = new ConcurrentHashMap<>();

    // Rate limiting per user (messages per second)
    private final Map<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Handlers are registered via WebSocketConfig
        // This class provides security configuration
    }

    public boolean canAcceptConnection(String userId) {
        if (userId == null) {
            log.warn("Connection rejected: null user ID");
            return false;
        }

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
        if (userId == null) return;

        AtomicInteger count = userConnectionCounts.get(userId);
        if (count != null) {
            int remaining = count.decrementAndGet();
            if (remaining <= 0) {
                userConnectionCounts.remove(userId);
                userRateLimiters.remove(userId);
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
        if (origin == null) return false;

        // In development, allow all origins
        if ("*".equals(allowedOrigins)) {
            return true;
        }

        // Check against configured allowed origins
        return allowedOrigins.contains(origin);
    }

    /**
     * Check if message rate is allowed for user.
     */
    public boolean allowMessage(String userId) {
        if (userId == null) return false;

        RateLimiter limiter = userRateLimiters.computeIfAbsent(userId,
                k -> new RateLimiter(rateLimitPerSecond, rateLimitPerSecond * 2));

        return limiter.tryAcquire();
    }

    /**
     * Get remaining rate limit tokens for user.
     */
    public int getRemainingRateLimit(String userId) {
        RateLimiter limiter = userRateLimiters.get(userId);
        return limiter != null ? limiter.getAvailableTokens() : rateLimitPerSecond;
    }

    /**
     * Rate limiter using token bucket algorithm.
     */
    private static class RateLimiter {
        private final int refillRate; // tokens per second
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;
        private final Object lock = new Object();

        RateLimiter(int refillRate, int capacity) {
            this.refillRate = refillRate;
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.nanoTime();
        }

        boolean tryAcquire() {
            refill();
            while (true) {
                int current = tokens.get();
                if (current <= 0) {
                    return false;
                }
                if (tokens.compareAndSet(current, current - 1)) {
                    return true;
                }
            }
        }

        int getAvailableTokens() {
            refill();
            return tokens.get();
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTime;

            if (elapsed > TimeUnit.SECONDS.toNanos(1)) {
                synchronized (lock) {
                    // Double-check after acquiring lock
                    elapsed = now - lastRefillTime;
                    if (elapsed > TimeUnit.SECONDS.toNanos(1)) {
                        long seconds = elapsed / TimeUnit.SECONDS.toNanos(1);
                        int add = (int) (seconds * refillRate);
                        int current = tokens.get();
                        int newTokens = Math.min(capacity, current + add);
                        tokens.set(newTokens);
                        lastRefillTime = now;
                    }
                }
            }
        }
    }

    /**
     * WebSocket interceptor for enforcing rate limits.
     */
    @Component
    public static class WebSocketRateLimitInterceptor implements ChannelInterceptor {

        private final WebSocketSecurityConfig securityConfig;

        public WebSocketRateLimitInterceptor(WebSocketSecurityConfig securityConfig) {
            this.securityConfig = securityConfig;
        }

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

            if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
                String userId = accessor.getFirstNativeHeader("userId");
                if (userId != null && !securityConfig.allowMessage(userId)) {
                    log.warn("Rate limit exceeded for user: {}", userId);
                    throw new IllegalStateException("Rate limit exceeded");
                }
            }

            return message;
        }
    }
}