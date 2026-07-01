package com.jira.test.controller;

import com.jira.test.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class WebSocketEventController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Subscribe to project-specific test events
     * Client subscribes to: /topic/test-events/{projectId}
     */
    @GetMapping("/api/ws/subscribe/{projectId}")
    public Map<String, Object> getSubscriptionInfo(@PathVariable UUID projectId) {
        return Map.of(
                "endpoint", "/ws/test-events",
                "topic", "/topic/test-events/" + projectId,
                "messageFormat", "STOMP"
        );
    }

    /**
     * Send event to specific project channel
     * Message format: send to /app/test-event with projectId header
     */
    @MessageMapping("/test-event")
    @SendTo("/topic/test-events")
    public Map<String, Object> handleTestEvent(@Payload Map<String, Object> eventData) {
        log.info("Received test event via WebSocket: {}", eventData);
        return Map.of(
                "status", "received",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Send event to project-specific channel
     */
    @MessageMapping("/test-event/{projectId}")
    public void sendToProjectChannel(@Payload Map<String, Object> eventData,
                                      @DestinationVariable UUID projectId) {
        log.info("Sending event to project channel: {} - {}", projectId, eventData);
        eventData.put("projectId", projectId);
        messagingTemplate.convertAndSend("/topic/test-events/" + projectId, eventData);
    }

    /**
     * Broadcast to all connected clients
     */
    @PostMapping("/api/ws/broadcast")
    public Map<String, Object> broadcastEvent(@RequestBody Map<String, Object> eventData) {
        log.info("Broadcasting event to all clients: {}", eventData);
        messagingTemplate.convertAndSend("/topic/test-events", eventData);
        return Map.of(
                "status", "broadcast",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Send to specific project
     */
    @PostMapping("/api/ws/send/{projectId}")
    public Map<String, Object> sendToProject(@PathVariable UUID projectId,
                                              @RequestBody Map<String, Object> eventData) {
        log.info("Sending event to project: {} - {}", projectId, eventData);
        eventData.put("projectId", projectId);
        eventData.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/test-events/" + projectId, eventData);
        return Map.of(
                "status", "sent",
                "projectId", projectId,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * Dashboard update endpoint
     */
    @PostMapping("/api/ws/dashboard/{projectId}")
    public Map<String, Object> updateDashboard(@PathVariable UUID projectId,
                                               @RequestBody Map<String, Object> dashboardData) {
        log.info("Sending dashboard update for project: {}", projectId);
        dashboardData.put("projectId", projectId);
        dashboardData.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/dashboard/" + projectId, dashboardData);
        return Map.of(
                "status", "dashboard_updated",
                "projectId", projectId,
                "timestamp", System.currentTimeMillis()
        );
    }
}