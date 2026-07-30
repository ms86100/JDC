package com.avionics_systems.migration.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.dto.JobProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw WebSocket handler for clients that need direct WebSocket access
 * (without STOMP protocol)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketConnectionManager extends TextWebSocketHandler {

    private final MigrationWebSocketHandler migrationWebSocketHandler;
    private final ReconnectionHandler reconnectionHandler;
    private final ObjectMapper objectMapper;

    // Track active sessions: sessionId -> session
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // Track session metadata: sessionId -> metadata
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    // Heartbeat interval in milliseconds
    private static final long HEARTBEAT_INTERVAL_MS = 25000;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userId = extractUserId(session);

        activeSessions.put(sessionId, session);
        sessionMetadata.put(sessionId, new SessionMetadata(userId, Instant.now()));

        migrationWebSocketHandler.registerConnection(sessionId, userId);

        // Send connection confirmation
        Map<String, Object> connectionEvent = Map.of(
                "eventType", "CONNECTION_ESTABLISHED",
                "sessionId", sessionId,
                "userId", userId,
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectionEvent)));

        log.info("Raw WebSocket connection established: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();

        try {
            // Parse incoming message
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) payload.get("action");

            switch (action) {
                case "subscribe" -> handleSubscribe(session, payload);
                case "unsubscribe" -> handleUnsubscribe(session, payload);
                case "heartbeat" -> handleHeartbeat(session);
                case "status" -> handleStatusRequest(session, payload);
                default -> log.warn("Unknown action: {}", action);
            }

            // Update last event ID for reconnection
            String eventId = (String) payload.get("eventId");
            if (eventId != null) {
                reconnectionHandler.updateLastEventId(sessionId, eventId);
            }

        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", sessionId, e.getMessage());
            sendError(session, "MESSAGE_PARSE_ERROR", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();

        activeSessions.remove(sessionId);
        sessionMetadata.remove(sessionId);

        migrationWebSocketHandler.removeConnection(sessionId);
        reconnectionHandler.onConnectionLost(sessionId);

        log.info("Raw WebSocket connection closed: {}, status: {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        reconnectionHandler.onConnectionLost(session.getId());
    }

    private void handleSubscribe(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String sessionId = session.getId();
        String jobId = (String) payload.get("jobId");

        if (jobId == null || jobId.isBlank()) {
            sendError(session, "INVALID_SUBSCRIBE", "jobId is required");
            return;
        }

        migrationWebSocketHandler.subscribeToJob(sessionId, jobId);

        Map<String, Object> response = Map.of(
                "eventType", "SUBSCRIBED",
                "jobId", jobId,
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        log.info("Session {} subscribed to job {}", sessionId, jobId);
    }

    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> payload) throws IOException {
        String sessionId = session.getId();
        String jobId = (String) payload.get("jobId");

        if (jobId != null) {
            migrationWebSocketHandler.unsubscribeFromJob(sessionId, jobId);
        }

        Map<String, Object> response = Map.of(
                "eventType", "UNSUBSCRIBED",
                "jobId", jobId,
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleHeartbeat(WebSocketSession session) throws IOException {
        Map<String, Object> response = Map.of(
                "eventType", "HEARTBEAT",
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleStatusRequest(WebSocketSession session, Map<String, Object> payload) throws IOException {
        // Return current status - this would typically query the PollingFallbackService
        Map<String, Object> response = Map.of(
                "eventType", "STATUS_RESPONSE",
                "activeConnections", activeSessions.size(),
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void sendError(WebSocketSession session, String errorCode, String message) throws IOException {
        Map<String, Object> error = Map.of(
                "eventType", "ERROR",
                "errorCode", errorCode,
                "message", message,
                "timestamp", Instant.now().toString()
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }

    private String extractUserId(WebSocketSession session) {
        // Extract from query parameter or headers
        String query = session.getUri() != null ? session.getUri().getQuery() : "";
        if (query.contains("userId=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("userId=")) {
                    return param.substring(7);
                }
            }
        }
        return session.getId(); // Fallback to session ID
    }

    public int getActiveConnectionCount() {
        return activeSessions.size();
    }

    public Set<String> getActiveSessionIds() {
        return Set.copyOf(activeSessions.keySet());
    }

    // Session metadata record
    private record SessionMetadata(String userId, Instant connectedAt) {}

    // Scheduled heartbeat sender
    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 25000)
    public void sendHeartbeats() {
        for (WebSocketSession session : activeSessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new PingMessage());
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat to session {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        }
    }
}