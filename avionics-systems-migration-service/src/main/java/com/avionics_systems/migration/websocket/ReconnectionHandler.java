package com.avionics_systems.migration.websocket;

import com.avionics_systems.migration.websocket.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles reconnection logic and tracks missed events for clients
 */
@Component
@Slf4j
public class ReconnectionHandler {

    // Track last event ID for each session (for event replay)
    private final Map<String, String> lastEventIds = new ConcurrentHashMap<>();

    // Track reconnection attempts per session
    private final Map<String, AtomicInteger> reconnectAttempts = new ConcurrentHashMap<>();

    // Reconnection configuration
    @Value("${app.websocket.max-reconnect-attempts:5}")
    private int maxReconnectAttempts;

    @Value("${app.websocket.reconnect-delay-ms:1000}")
    private long reconnectDelayMs;

    /**
     * On reconnect, record the session's last known event ID
     */
    public void onReconnected(String sessionId, String userId, String lastEventId) {
        lastEventIds.put(sessionId, lastEventId);
        reconnectAttempts.remove(sessionId);
        log.info("Session {} reconnected with last event ID: {}", sessionId, lastEventId);
    }

    /**
     * Handle connection lost
     */
    public void onConnectionLost(String sessionId) {
        AtomicInteger attempts = reconnectAttempts.computeIfAbsent(sessionId, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts <= maxReconnectAttempts) {
            log.info("Connection lost for session {}, attempt {}/{}",
                    sessionId, currentAttempts, maxReconnectAttempts);
        } else {
            log.warn("Max reconnection attempts reached for session {}", sessionId);
        }
    }

    /**
     * Check if client can reconnect
     */
    public boolean canReconnect(String sessionId) {
        AtomicInteger attempts = reconnectAttempts.get(sessionId);
        return attempts == null || attempts.get() < maxReconnectAttempts;
    }

    /**
     * Get delay for next reconnection attempt
     */
    public long getReconnectDelay(String sessionId) {
        AtomicInteger attempts = reconnectAttempts.get(sessionId);
        int currentAttempts = attempts != null ? attempts.get() : 0;

        // Exponential backoff with jitter
        long baseDelay = reconnectDelayMs * (1L << Math.min(currentAttempts, 6));
        long jitter = (long) (Math.random() * baseDelay * 0.1);

        return Math.min(baseDelay + jitter, 30000); // Cap at 30 seconds
    }

    /**
     * Get last event ID for session
     */
    public String getLastEventId(String sessionId) {
        return lastEventIds.get(sessionId);
    }

    /**
     * Update last event ID for session
     */
    public void updateLastEventId(String sessionId, String eventId) {
        lastEventIds.put(sessionId, eventId);
    }

    /**
     * Clear session data
     */
    public void clearSession(String sessionId) {
        lastEventIds.remove(sessionId);
        reconnectAttempts.remove(sessionId);
    }

    /**
     * Get reconnection status for monitoring
     */
    public Map<String, Integer> getReconnectionStatus() {
        return Map.copyOf(reconnectAttempts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                )));
    }
}