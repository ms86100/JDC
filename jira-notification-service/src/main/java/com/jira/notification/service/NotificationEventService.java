package com.jira.notification.service;

import com.jira.notification.dto.CreateNotificationEventRequest;
import com.jira.notification.dto.NotificationEventResponse;
import com.jira.notification.entity.NotificationEventEntity;
import com.jira.notification.exception.ResourceNotFoundException;
import com.jira.notification.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventService {

    private final NotificationEventRepository eventRepository;

    @Value("${app.notification.event-categories.issue:Issue}")
    private String categoryIssue;

    @Value("${app.notification.event-categories.comment:Comment}")
    private String categoryComment;

    @Value("${app.notification.event-categories.status:Status}")
    private String categoryStatus;

    @Value("${app.notification.event-categories.sprint:Sprint}")
    private String categorySprint;

    @Value("${app.notification.event-categories.project:Project}")
    private String categoryProject;

    @Transactional
    public NotificationEventResponse createEvent(CreateNotificationEventRequest request) {
        log.info("Creating notification event: {}", request.getEventType());

        if (eventRepository.existsByEventType(request.getEventType())) {
            throw new IllegalArgumentException("Event type '" + request.getEventType() + "' already exists");
        }

        NotificationEventEntity event = NotificationEventEntity.builder()
                .eventType(request.getEventType())
                .name(request.getName())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .category(request.getCategory())
                .iconUrl(request.getIconUrl())
                .isSystemEvent(request.getIsSystemEvent() != null ? request.getIsSystemEvent() : false)
                .build();

        event = eventRepository.save(event);
        log.info("Created notification event with id: {}", event.getId());

        return mapToResponse(event);
    }

    @Transactional(readOnly = true)
    public NotificationEventResponse getEvent(UUID eventId) {
        log.debug("Fetching notification event: {}", eventId);

        NotificationEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification event not found: " + eventId));

        return mapToResponse(event);
    }

    @Transactional(readOnly = true)
    public NotificationEventResponse getEventByType(String eventType) {
        log.debug("Fetching notification event by type: {}", eventType);

        NotificationEventEntity event = eventRepository.findByEventType(eventType)
                .orElseThrow(() -> new ResourceNotFoundException("Notification event not found: " + eventType));

        return mapToResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEventResponse> getAllEvents(int page, int size) {
        log.debug("Fetching all notification events");
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationEventResponse> getEventsByCategory(String category) {
        log.debug("Fetching notification events by category: {}", category);
        return eventRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationEventResponse> getActiveEvents() {
        log.debug("Fetching all active notification events");
        return eventRepository.findAllActiveEvents().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationEventResponse> getSystemEvents() {
        log.debug("Fetching system notification events");
        return eventRepository.findSystemEvents().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationEventResponse updateEvent(UUID eventId, CreateNotificationEventRequest request) {
        log.info("Updating notification event: {}", eventId);

        NotificationEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification event not found: " + eventId));

        if (!event.getEventType().equals(request.getEventType()) &&
                eventRepository.existsByEventType(request.getEventType())) {
            throw new IllegalArgumentException("Event type '" + request.getEventType() + "' already exists");
        }

        event.setEventType(request.getEventType());
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        if (request.getEnabled() != null) {
            event.setEnabled(request.getEnabled());
        }
        event.setCategory(request.getCategory());
        event.setIconUrl(request.getIconUrl());

        event = eventRepository.save(event);
        log.info("Updated notification event: {}", eventId);

        return mapToResponse(event);
    }

    @Transactional
    public void deleteEvent(UUID eventId) {
        log.info("Deleting notification event: {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Notification event not found: " + eventId);
        }

        eventRepository.deleteById(eventId);
        log.info("Deleted notification event: {}", eventId);
    }

    @Transactional
    public NotificationEventResponse toggleEvent(UUID eventId, boolean enabled) {
        log.info("Toggling notification event {} to enabled={}", eventId, enabled);

        NotificationEventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification event not found: " + eventId));

        event.setEnabled(enabled);
        event = eventRepository.save(event);

        return mapToResponse(event);
    }

    @Transactional
    public void initializeDefaultEvents() {
        log.info("Initializing default notification events");

        List<NotificationEventEntity> defaultEvents = Arrays.asList(
                NotificationEventEntity.builder()
                        .eventType("ISSUE_CREATED")
                        .name("Issue Created")
                        .description("Triggered when a new issue is created")
                        .category(categoryIssue)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_UPDATED")
                        .name("Issue Updated")
                        .description("Triggered when an issue is updated")
                        .category(categoryIssue)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_DELETED")
                        .name("Issue Deleted")
                        .description("Triggered when an issue is deleted")
                        .category(categoryIssue)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_ASSIGNED")
                        .name("Issue Assigned")
                        .description("Triggered when an issue is assigned to a user")
                        .category(categoryIssue)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_COMMENTED")
                        .name("Issue Commented")
                        .description("Triggered when a comment is added to an issue")
                        .category(categoryComment)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_RESOLVED")
                        .name("Issue Resolved")
                        .description("Triggered when an issue is resolved")
                        .category(categoryStatus)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_CLOSED")
                        .name("Issue Closed")
                        .description("Triggered when an issue is closed")
                        .category(categoryStatus)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("ISSUE_REOPENED")
                        .name("Issue Reopened")
                        .description("Triggered when an issue is reopened")
                        .category(categoryStatus)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("SPRINT_STARTED")
                        .name("Sprint Started")
                        .description("Triggered when a sprint begins")
                        .category(categorySprint)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("SPRINT_COMPLETED")
                        .name("Sprint Completed")
                        .description("Triggered when a sprint ends")
                        .category(categorySprint)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("SPRINT_CANCELLED")
                        .name("Sprint Cancelled")
                        .description("Triggered when a sprint is cancelled")
                        .category(categorySprint)
                        .isSystemEvent(true)
                        .build(),
                NotificationEventEntity.builder()
                        .eventType("PROJECT_CREATED")
                        .name("Project Created")
                        .description("Triggered when a new project is created")
                        .category(categoryProject)
                        .isSystemEvent(true)
                        .build()
        );

        for (NotificationEventEntity event : defaultEvents) {
            if (!eventRepository.existsByEventType(event.getEventType())) {
                eventRepository.save(event);
                log.info("Initialized default event: {}", event.getEventType());
            }
        }
    }

    private NotificationEventResponse mapToResponse(NotificationEventEntity event) {
        return NotificationEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .name(event.getName())
                .description(event.getDescription())
                .enabled(event.getEnabled())
                .category(event.getCategory())
                .iconUrl(event.getIconUrl())
                .isSystemEvent(event.getIsSystemEvent())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
