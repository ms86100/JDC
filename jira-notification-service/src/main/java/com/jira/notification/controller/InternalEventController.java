package com.jira.notification.controller;

import com.jira.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalEventController {

    private final NotificationDispatchService dispatchService;

    @PostMapping("/issue-event")
    public ResponseEntity<Map<String, Object>> handleIssueEvent(@RequestBody Map<String, Object> payload) {
        String eventType = (String) payload.get("eventType");
        String issueIdStr = (String) payload.get("issueId");
        String projectIdStr = (String) payload.get("projectId");
        String title = (String) payload.get("title");
        String message = (String) payload.get("message");

        UUID issueId = issueIdStr != null ? parseUuid(issueIdStr) : null;
        UUID projectId = projectIdStr != null ? parseUuid(projectIdStr) : null;

        log.info("Received issue event: type={}, issueId={}, projectId={}", eventType, issueId, projectId);

        try {
            dispatchService.dispatchIssueEvent(eventType, issueId, projectId, title, message);
            return ResponseEntity.ok(Map.of("status", "dispatched", "eventType", eventType != null ? eventType : ""));
        } catch (Exception e) {
            log.error("Failed to dispatch issue event: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
