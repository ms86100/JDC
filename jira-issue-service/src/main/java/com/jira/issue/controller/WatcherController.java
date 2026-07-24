package com.jira.issue.controller;

import com.jira.issue.dto.WatcherResponse;
import com.jira.issue.service.WatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing issue watchers
 */
@RestController
@RequestMapping("/api/issues/{issueId}/watchers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Issue Watchers", description = "Watcher management for issues")
public class WatcherController {

    private final WatcherService watcherService;

    @PostMapping
    @Operation(summary = "Add watcher to issue", description = "Start watching an issue")
    public ResponseEntity<WatcherResponse> addWatcher(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Adding watcher for issue {} by user {}", issueId, userId);
        WatcherResponse response = watcherService.addWatcher(issueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping
    @Operation(summary = "Remove watcher from issue", description = "Stop watching an issue")
    public ResponseEntity<Void> removeWatcher(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Removing watcher for issue {} by user {}", issueId, userId);
        watcherService.removeWatcher(issueId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get watchers for issue", description = "Get all watchers for an issue")
    public ResponseEntity<List<WatcherResponse>> getWatchers(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        log.info("Getting watchers for issue: {}", issueId);
        List<WatcherResponse> watchers = watcherService.getWatchersByIssue(issueId);
        return ResponseEntity.ok(watchers);
    }

    @GetMapping("/count")
    @Operation(summary = "Get watcher count", description = "Get the number of watchers for an issue")
    public ResponseEntity<Long> getWatcherCount(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId) {
        log.info("Getting watcher count for issue: {}", issueId);
        long count = watcherService.getWatcherCount(issueId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/check")
    @Operation(summary = "Check if user is watching", description = "Check if the current user is watching")
    public ResponseEntity<Boolean> isWatching(
            @Parameter(description = "Issue ID") @PathVariable UUID issueId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") UUID userId) {
        log.info("Checking if user {} is watching issue {}", userId, issueId);
        boolean watching = watcherService.isWatching(issueId, userId);
        return ResponseEntity.ok(watching);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get watched issues by user", description = "Get all issues watched by a specific user")
    public ResponseEntity<List<WatcherResponse>> getWatchedIssues(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        log.info("Getting watched issues by user: {}", userId);
        List<WatcherResponse> watched = watcherService.getWatchedIssuesByUser(userId);
        return ResponseEntity.ok(watched);
    }
}