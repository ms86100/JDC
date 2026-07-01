package com.jira.notification.controller;

import com.jira.notification.dto.CreateNotificationEventRequest;
import com.jira.notification.dto.NotificationEventResponse;
import com.jira.notification.service.NotificationEventService;
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
@RequestMapping("/api/notification-events")
@RequiredArgsConstructor
@Slf4j
public class NotificationEventController {

    private final NotificationEventService eventService;

    @PostMapping
    public ResponseEntity<NotificationEventResponse> createEvent(@Valid @RequestBody CreateNotificationEventRequest request) {
        log.info("POST /api/notification-events - Creating notification event: {}", request.getEventType());
        NotificationEventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationEventResponse> getEvent(@PathVariable UUID id) {
        log.info("GET /api/notification-events/{} - Fetching notification event", id);
        NotificationEventResponse response = eventService.getEvent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<NotificationEventResponse> getEventByType(@PathVariable String eventType) {
        log.info("GET /api/notification-events/type/{} - Fetching event by type", eventType);
        NotificationEventResponse response = eventService.getEventByType(eventType);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationEventResponse>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/notification-events - Fetching all notification events");
        Page<NotificationEventResponse> response = eventService.getAllEvents(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<NotificationEventResponse>> getEventsByCategory(@PathVariable String category) {
        log.info("GET /api/notification-events/category/{} - Fetching events by category", category);
        List<NotificationEventResponse> response = eventService.getEventsByCategory(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<NotificationEventResponse>> getActiveEvents() {
        log.info("GET /api/notification-events/active - Fetching all active events");
        List<NotificationEventResponse> response = eventService.getActiveEvents();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/system")
    public ResponseEntity<List<NotificationEventResponse>> getSystemEvents() {
        log.info("GET /api/notification-events/system - Fetching system events");
        List<NotificationEventResponse> response = eventService.getSystemEvents();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationEventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody CreateNotificationEventRequest request) {
        log.info("PUT /api/notification-events/{} - Updating notification event", id);
        NotificationEventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
        log.info("DELETE /api/notification-events/{} - Deleting notification event", id);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<NotificationEventResponse> toggleEvent(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        log.info("PATCH /api/notification-events/{}/toggle - Toggling event to enabled={}", id, enabled);
        NotificationEventResponse response = eventService.toggleEvent(id, enabled);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize")
    public ResponseEntity<Void> initializeDefaultEvents() {
        log.info("POST /api/notification-events/initialize - Initializing default events");
        eventService.initializeDefaultEvents();
        return ResponseEntity.ok().build();
    }
}