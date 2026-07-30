package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.dto.JobProgressResponse;
import com.avionics_systems.migration.service.PollingFallbackService;
import com.avionics_systems.migration.service.MigrationService;
import com.avionics_systems.migration.websocket.MigrationWebSocketHandler;
import com.avionics_systems.migration.websocket.ReconnectionHandler;
import com.avionics_systems.migration.websocket.dto.JobProgressUpdate;
import com.avionics_systems.migration.websocket.dto.WebSocketMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ws")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket", description = "WebSocket subscription and status endpoints")
public class WebSocketController {

    private final MigrationWebSocketHandler webSocketHandler;
    private final ReconnectionHandler reconnectionHandler;
    private final PollingFallbackService pollingFallbackService;
    private final MigrationService migrationService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/job/{jobId}/subscribe")
    @Operation(summary = "Subscribe to job progress updates via STOMP")
    public void subscribeToJob(
            @DestinationVariable String jobId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId = extractUserId(headerAccessor);
        String sessionId = headerAccessor.getSessionId();

        webSocketHandler.subscribeToJob(sessionId, jobId);

        // Send initial status
        JobProgressUpdate progress = getCurrentStatus(jobId).getBody();
        if (progress != null) {
            webSocketHandler.sendProgressUpdate(jobId, "anonymous", progress);
        }

        log.info("User {} subscribed to job {} (session: {})", userId, jobId, sessionId);
    }

    @MessageMapping("/job/{jobId}/unsubscribe")
    @Operation(summary = "Unsubscribe from job progress updates")
    public void unsubscribeFromJob(
            @DestinationVariable String jobId,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        webSocketHandler.unsubscribeFromJob(sessionId, jobId);

        log.info("Session {} unsubscribed from job {}", sessionId, jobId);
    }

    @GetMapping("/job/{jobId}/status")
    @Operation(summary = "Get current status for a job (for polling fallback)")
    public ResponseEntity<JobProgressUpdate> getCurrentStatus(
            @PathVariable String jobId) {

        Optional<JobProgressUpdate> cached = pollingFallbackService.getCachedProgress(jobId);

        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        // Try to get from migration service and cache it
        try {
            UUID jobUuid = UUID.fromString(jobId);
            JobProgressResponse progress = migrationService.getJobProgress(jobUuid);

            if (progress != null) {
                JobProgressUpdate update = JobProgressUpdate.fromProgressResponse(progress);
                pollingFallbackService.cacheProgress(jobId, update);
                return ResponseEntity.ok(update);
            }

            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/connections/stats")
    @Operation(summary = "Get WebSocket connection statistics")
    public ResponseEntity<Map<String, Object>> getConnectionStats() {
        return ResponseEntity.ok(Map.of(
                "activeConnections", webSocketHandler.getActiveConnectionCount(),
                "reconnectionStatus", reconnectionHandler.getReconnectionStatus(),
                "cacheStats", pollingFallbackService.getCacheStats()
        ));
    }

    @GetMapping("/job/{jobId}/subscribers")
    @Operation(summary = "Get subscriber count for a job")
    public ResponseEntity<Map<String, Object>> getJobSubscribers(
            @PathVariable String jobId) {

        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "subscriberCount", webSocketHandler.getJobSubscriberCount(jobId)
        ));
    }

    // POST endpoint for clients that prefer REST for subscription
    @PostMapping("/job/{jobId}/subscribe")
    @Operation(summary = "Subscribe to job updates via REST (alternative to STOMP)")
    public ResponseEntity<Map<String, Object>> subscribeViaRest(
            @PathVariable String jobId,
            @RequestParam String userId) {

        // For REST-based subscription, we'd need to track differently
        // This is primarily for long-polling fallback
        log.info("REST subscription request for job {} by user {}", jobId, userId);

        return ResponseEntity.ok(Map.of(
                "subscribed", true,
                "jobId", jobId,
                "userId", userId,
                "pollEndpoint", "/api/ws/job/" + jobId + "/status"
        ));
    }

    @DeleteMapping("/job/{jobId}/subscribe")
    @Operation(summary = "Unsubscribe from job updates via REST")
    public ResponseEntity<Map<String, Object>> unsubscribeViaRest(
            @PathVariable String jobId,
            @RequestParam String userId) {

        pollingFallbackService.invalidateCache(jobId);

        return ResponseEntity.ok(Map.of(
                "unsubscribed", true,
                "jobId", jobId
        ));
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Send heartbeat to keep connection alive")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @RequestParam(defaultValue = "system") String userId) {

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "timestamp", System.currentTimeMillis(),
                "activeConnections", webSocketHandler.getActiveConnectionCount()
        ));
    }

    private String extractUserId(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            return principal.getName();
        }

        // Try to get from headers
        String userId = headerAccessor.getSessionAttributes().get("userId") != null
                ? headerAccessor.getSessionAttributes().get("userId").toString()
                : "anonymous";

        return userId;
    }
}