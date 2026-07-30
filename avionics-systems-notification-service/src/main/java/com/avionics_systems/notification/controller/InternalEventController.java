package com.avionics_systems.notification.controller;

import com.avionics_systems.notification.dto.NotificationDispatchRequest;
import com.avionics_systems.notification.service.NotificationDispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final NotificationDispatchService dispatchService;

    @PostMapping("/api/notifications/internal/issue-event")
    public ResponseEntity<Map<String, Object>> handleIssueEvent(@RequestBody Map<String, Object> payload) {
        String eventType = (String) payload.get("eventType");
        if (eventType == null || eventType.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "eventType is required"));
        }

        UUID issueId = parseUuid((String) payload.get("issueId"));
        UUID projectId = parseUuid((String) payload.get("projectId"));
        UUID actorUserId = parseUuid((String) payload.get("actorUserId"));
        String title = (String) payload.get("title");
        String message = (String) payload.get("message");

        log.info("Received issue event: type={}, issueId={}, projectId={}", eventType, issueId, projectId);

        try {
            dispatchService.dispatchIssueEvent(eventType, issueId, projectId, title, message, actorUserId);
            return ResponseEntity.ok(Map.of("status", "dispatched", "eventType", eventType));
        } catch (Exception e) {
            log.error("Failed to dispatch issue event {}: {}", eventType, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/api/notification-events/dispatch")
    public ResponseEntity<Map<String, Object>> dispatchEvent(@Valid @RequestBody NotificationDispatchRequest request) {
        log.info("Dispatch event: type={}, issueId={}, projectId={}, actor={}",
                request.getEventType(), request.getIssueId(), request.getProjectId(), request.getActorUserId());

        try {
            dispatchService.dispatchIssueEvent(
                    request.getEventType(), request.getIssueId(), request.getProjectId(),
                    request.getTitle(), request.getMessage(), request.getActorUserId());
            return ResponseEntity.ok(Map.of("status", "dispatched", "eventType", request.getEventType()));
        } catch (Exception e) {
            log.error("Failed to dispatch event {}: {}", request.getEventType(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
