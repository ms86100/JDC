package com.avionics_systems.migration.websocket;

import com.avionics_systems.migration.config.WebSocketSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final WebSocketSecurityConfig securityConfig;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = extractUserId(accessor);
            String sessionId = accessor.getSessionId();

            log.debug("WebSocket connection attempt - session: {}, user: {}", sessionId, userId);

            // Validate connection limits
            if (!securityConfig.canAcceptConnection(userId)) {
                log.warn("Connection rejected for user {}: connection limit exceeded", userId);
                throw new IllegalStateException("Too many connections");
            }

            // Extract and validate origin
            List<String> origins = accessor.getNativeHeader("Origin");
            if (origins != null && !origins.isEmpty()) {
                String origin = origins.get(0);
                if (!securityConfig.isOriginAllowed(origin)) {
                    log.warn("Connection rejected for user {}: origin not allowed: {}", userId, origin);
                    throw new IllegalStateException("Origin not allowed");
                }
            }

            log.info("WebSocket connection established - session: {}, user: {}", sessionId, userId);
        }

        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel,
                                     boolean sent, Exception ex) {
        if (ex != null) {
            log.error("Error sending WebSocket message", ex);
        }
    }

    private String extractUserId(StompHeaderAccessor accessor) {
        // Try to get user ID from headers
        List<String> userIdHeaders = accessor.getNativeHeader("userId");
        if (userIdHeaders != null && !userIdHeaders.isEmpty()) {
            return userIdHeaders.get(0);
        }

        // Fallback to principal name if available
        if (accessor.getUser() != null) {
            return accessor.getUser().getName();
        }

        return "anonymous";
    }
}