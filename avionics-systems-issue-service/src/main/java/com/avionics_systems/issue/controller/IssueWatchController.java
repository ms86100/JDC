package com.avionics_systems.issue.controller;

import com.avionics_systems.issue.service.WatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for watching issues.
 * Endpoints match frontend expectations at /api/issues/{id}/watch
 */
@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Watching", description = "Watch/unwatch issues for notifications")
public class IssueWatchController {

    private final WatcherService watcherService;

    @PostMapping("/{issueId}/watch")
    @Operation(summary = "Watch an issue", description = "Start watching an issue to receive notifications")
    public ResponseEntity<Void> watch(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("User {} watching issue {}", userId, issueId);
        if (userId != null) {
            watcherService.addWatcher(issueId, userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{issueId}/watch")
    @Operation(summary = "Stop watching an issue", description = "Stop receiving notifications for this issue")
    public ResponseEntity<Void> unwatch(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("User {} unwatching issue {}", userId, issueId);
        if (userId != null) {
            watcherService.removeWatcher(issueId, userId);
        }
        return ResponseEntity.noContent().build();
    }
}