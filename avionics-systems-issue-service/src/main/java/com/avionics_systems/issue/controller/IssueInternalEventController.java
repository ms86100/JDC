package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.dto.IssueEventBroadcastRequest;
import com.avionics_systems.issue.event.IssueEventOutboxPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/issue-events")
@RequiredArgsConstructor
@Tag(name = "Issue internal events", description = "Realtime broadcast hooks for workflow/migration services")
public class IssueInternalEventController {

    private final IssueEventOutboxPublisher issueEventOutboxPublisher;

    @PostMapping
    @Operation(summary = "Broadcast issue event", description = "Publishes to outbox + WebSocket for cross-tab sync")
    public ResponseEntity<Map<String, String>> broadcast(@RequestBody IssueEventBroadcastRequest request) {
        String type = normalizeType(request.getType());
        issueEventOutboxPublisher.publish(type, request.getIssueId(), request.getProjectId());
        if ("issue.transitioned".equals(type) && request.getIssueId() != null) {
            issueEventOutboxPublisher.publish("issue.updated", request.getIssueId(), request.getProjectId());
        }
        return ResponseEntity.ok(Map.of("type", type, "status", "broadcast"));
    }

    private static String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "issue.updated";
        }
        String t = raw.trim();
        if (t.equals("ISSUE_TRANSITIONED") || t.equals("STATUS_CHANGED")) {
            return "issue.transitioned";
        }
        if (!t.contains(".")) {
            return "issue." + t.toLowerCase();
        }
        return t;
    }
}
