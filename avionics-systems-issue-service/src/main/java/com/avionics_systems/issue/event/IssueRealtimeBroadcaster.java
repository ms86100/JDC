package com.avionics_systems.issue.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.cluster.event.ClusterEventBus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class IssueRealtimeBroadcaster {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClusterEventBus clusterEventBus;

    private static final String CHANNEL = "issue-realtime";

    @PostConstruct
    public void init() {
        clusterEventBus.subscribe(CHANNEL, this::onRemoteMessage);
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void publish(String type, UUID issueId, UUID projectId) {
        Map<String, Object> payload = Map.of(
                "type", type,
                "issueId", issueId != null ? issueId.toString() : "",
                "projectId", projectId != null ? projectId.toString() : "");
        try {
            String json = objectMapper.writeValueAsString(payload);
            broadcastToLocalSessions(json);
            clusterEventBus.publish(CHANNEL, json);
        } catch (Exception e) {
            log.warn("Failed to broadcast issue event: {}", e.getMessage());
        }
    }

    private void onRemoteMessage(String json) {
        broadcastToLocalSessions(json);
    }

    private void broadcastToLocalSessions(String json) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (Exception e) {
                    log.debug("Failed to send to WebSocket session: {}", e.getMessage());
                }
            }
        }
    }
}
