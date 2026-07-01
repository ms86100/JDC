package com.jira.notification.controller;

import com.jira.notification.dto.*;
import com.jira.notification.service.NotificationSchemeService;
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
@RequestMapping("/api/notification-schemes")
@RequiredArgsConstructor
@Slf4j
public class NotificationSchemeController {

    private final NotificationSchemeService schemeService;

    @PostMapping
    public ResponseEntity<NotificationSchemeResponse> createScheme(
            @Valid @RequestBody CreateNotificationSchemeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("POST /api/notification-schemes - Creating notification scheme: {}", request.getName());
        UUID createdBy = userId != null ? userId : UUID.randomUUID();
        NotificationSchemeResponse response = schemeService.createScheme(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationSchemeResponse> getScheme(@PathVariable UUID id) {
        log.info("GET /api/notification-schemes/{} - Fetching notification scheme", id);
        NotificationSchemeResponse response = schemeService.getScheme(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationSchemeResponse>> getAllSchemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/notification-schemes - Fetching all notification schemes");
        Page<NotificationSchemeResponse> response = schemeService.getAllSchemes(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<NotificationSchemeResponse>> getSchemesByProject(@PathVariable UUID projectId) {
        log.info("GET /api/notification-schemes/project/{} - Fetching schemes for project", projectId);
        List<NotificationSchemeResponse> response = schemeService.getSchemesByProject(projectId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationSchemeResponse> updateScheme(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNotificationSchemeRequest request) {
        log.info("PUT /api/notification-schemes/{} - Updating notification scheme", id);
        NotificationSchemeResponse response = schemeService.updateScheme(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheme(@PathVariable UUID id) {
        log.info("DELETE /api/notification-schemes/{} - Deleting notification scheme", id);
        schemeService.deleteScheme(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{schemeId}/events")
    public ResponseEntity<NotificationSchemeEventResponse> addSchemeEvent(
            @PathVariable UUID schemeId,
            @Valid @RequestBody CreateNotificationSchemeEventRequest request) {
        log.info("POST /api/notification-schemes/{}/events - Adding event to scheme", schemeId);
        NotificationSchemeEventResponse response = schemeService.addSchemeEvent(schemeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{schemeId}/events")
    public ResponseEntity<List<NotificationSchemeEventResponse>> getSchemeEvents(@PathVariable UUID schemeId) {
        log.info("GET /api/notification-schemes/{}/events - Fetching events for scheme", schemeId);
        List<NotificationSchemeEventResponse> response = schemeService.getSchemeEvents(schemeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{schemeId}/events/{eventId}")
    public ResponseEntity<NotificationSchemeEventResponse> updateSchemeEvent(
            @PathVariable UUID schemeId,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateNotificationSchemeEventRequest request) {
        log.info("PUT /api/notification-schemes/{}/events/{} - Updating event", schemeId, eventId);
        NotificationSchemeEventResponse response = schemeService.updateSchemeEvent(schemeId, eventId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{schemeId}/events/{eventId}")
    public ResponseEntity<Void> deleteSchemeEvent(
            @PathVariable UUID schemeId,
            @PathVariable UUID eventId) {
        log.info("DELETE /api/notification-schemes/{}/events/{} - Deleting event", schemeId, eventId);
        schemeService.deleteSchemeEvent(schemeId, eventId);
        return ResponseEntity.noContent().build();
    }
}