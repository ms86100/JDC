package com.jira.notification.controller;

import com.jira.notification.dto.*;
import com.jira.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@Valid @RequestBody NotificationEvent event) {
        log.info("POST /api/notifications - Creating notification for user: {}", event.getUserId());
        NotificationResponse response = notificationService.createNotification(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam UUID userId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/notifications - Fetching notifications for user: {}", userId);
        Page<NotificationResponse> response = notificationService.getNotifications(userId, read, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID id) {
        log.info("PUT /api/notifications/{}/read - Marking as read", id);
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestParam UUID userId) {
        log.info("PUT /api/notifications/read-all - Marking all as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam UUID userId) {
        log.info("GET /api/notifications/unread-count - Getting count for user: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences(@PathVariable UUID userId) {
        log.info("GET /api/notifications/preferences/{} - Fetching preferences", userId);
        List<NotificationPreferenceResponse> response = notificationService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/preferences/{userId}")
    public ResponseEntity<List<NotificationPreferenceResponse>> updatePreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        log.info("PUT /api/notifications/preferences/{} - Updating preferences", userId);
        List<NotificationPreferenceResponse> response = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        log.info("DELETE /api/notifications/{} - Deleting notification", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}