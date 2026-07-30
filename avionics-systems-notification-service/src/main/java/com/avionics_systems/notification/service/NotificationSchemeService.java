package com.avionics_systems.notification.service;

import com.avionics_systems.notification.dto.*;
import com.avionics_systems.notification.entity.NotificationScheme;
import com.avionics_systems.notification.entity.NotificationSchemeEvent;
import com.avionics_systems.notification.exception.ResourceNotFoundException;
import com.avionics_systems.notification.repository.NotificationSchemeEventRepository;
import com.avionics_systems.notification.repository.NotificationSchemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchemeService {

    private final NotificationSchemeRepository schemeRepository;
    private final NotificationSchemeEventRepository schemeEventRepository;

    @Transactional
    public NotificationSchemeResponse createScheme(CreateNotificationSchemeRequest request, UUID createdBy) {
        log.info("Creating notification scheme: {}", request.getName());

        if (schemeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Notification scheme with name '" + request.getName() + "' already exists");
        }

        Boolean isDefault = request.getIsDefault() != null ? request.getIsDefault() : false;
        if (isDefault && request.getProjectId() != null && schemeRepository.existsDefaultSchemeForProject(request.getProjectId())) {
            throw new IllegalArgumentException("Project already has a default notification scheme");
        }

        NotificationScheme scheme = NotificationScheme.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .createdBy(createdBy)
                .isDefault(isDefault)
                .build();

        scheme = schemeRepository.save(scheme);
        log.info("Created notification scheme with id: {}", scheme.getId());

        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public NotificationSchemeResponse getScheme(UUID schemeId) {
        log.debug("Fetching notification scheme: {}", schemeId);

        NotificationScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification scheme not found: " + schemeId));

        return mapToResponse(scheme);
    }

    @Transactional(readOnly = true)
    public Page<NotificationSchemeResponse> getAllSchemes(int page, int size) {
        log.debug("Fetching all notification schemes");
        Pageable pageable = PageRequest.of(page, size);
        return schemeRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<NotificationSchemeResponse> getSchemesByProject(UUID projectId) {
        log.debug("Fetching notification schemes for project: {}", projectId);
        return schemeRepository.findByProjectId(projectId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationSchemeResponse updateScheme(UUID schemeId, CreateNotificationSchemeRequest request) {
        log.info("Updating notification scheme: {}", schemeId);

        NotificationScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification scheme not found: " + schemeId));

        if (!scheme.getName().equals(request.getName()) && schemeRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Notification scheme with name '" + request.getName() + "' already exists");
        }

        Boolean isDefault = request.getIsDefault() != null ? request.getIsDefault() : false;
        if (isDefault && !scheme.getIsDefault() && request.getProjectId() != null &&
                schemeRepository.existsDefaultSchemeForProject(request.getProjectId())) {
            throw new IllegalArgumentException("Project already has a default notification scheme");
        }

        scheme.setName(request.getName());
        scheme.setDescription(request.getDescription());
        if (request.getProjectId() != null) {
            scheme.setProjectId(request.getProjectId());
        }
        scheme.setIsDefault(isDefault);

        scheme = schemeRepository.save(scheme);
        log.info("Updated notification scheme: {}", schemeId);

        return mapToResponse(scheme);
    }

    @Transactional
    public void deleteScheme(UUID schemeId) {
        log.info("Deleting notification scheme: {}", schemeId);

        if (!schemeRepository.existsById(schemeId)) {
            throw new ResourceNotFoundException("Notification scheme not found: " + schemeId);
        }

        schemeEventRepository.deleteAllBySchemeId(schemeId);
        schemeRepository.deleteById(schemeId);
        log.info("Deleted notification scheme: {}", schemeId);
    }

    @Transactional
    public NotificationSchemeEventResponse addSchemeEvent(UUID schemeId, CreateNotificationSchemeEventRequest request) {
        log.info("Adding event to notification scheme: {}", schemeId);

        NotificationScheme scheme = schemeRepository.findById(schemeId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification scheme not found: " + schemeId));

        NotificationSchemeEvent event = NotificationSchemeEvent.builder()
                .schemeId(schemeId)
                .eventType(request.getEventType())
                .recipientType(request.getRecipientType())
                .recipientId(request.getRecipientId())
                .recipientGroup(request.getRecipientGroup())
                .notificationTemplateId(request.getNotificationTemplateId())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .notifyAssignee(request.getNotifyAssignee() != null ? request.getNotifyAssignee() : false)
                .notifyReporter(request.getNotifyReporter() != null ? request.getNotifyReporter() : false)
                .notifyWatchers(request.getNotifyWatchers() != null ? request.getNotifyWatchers() : false)
                .notifyVoters(request.getNotifyVoters() != null ? request.getNotifyVoters() : false)
                .build();

        event = schemeEventRepository.save(event);
        log.info("Added event to scheme: {} - eventId: {}", schemeId, event.getId());

        return mapToEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<NotificationSchemeEventResponse> getSchemeEvents(UUID schemeId) {
        log.debug("Fetching events for notification scheme: {}", schemeId);

        if (!schemeRepository.existsById(schemeId)) {
            throw new ResourceNotFoundException("Notification scheme not found: " + schemeId);
        }

        return schemeEventRepository.findBySchemeId(schemeId).stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationSchemeEventResponse updateSchemeEvent(UUID schemeId, UUID eventId, CreateNotificationSchemeEventRequest request) {
        log.info("Updating event {} in scheme: {}", eventId, schemeId);

        NotificationSchemeEvent event = schemeEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!event.getSchemeId().equals(schemeId)) {
            throw new IllegalArgumentException("Event does not belong to this notification scheme");
        }

        event.setEventType(request.getEventType());
        event.setRecipientType(request.getRecipientType());
        event.setRecipientId(request.getRecipientId());
        event.setRecipientGroup(request.getRecipientGroup());
        event.setNotificationTemplateId(request.getNotificationTemplateId());
        if (request.getEnabled() != null) {
            event.setEnabled(request.getEnabled());
        }
        event.setNotifyAssignee(request.getNotifyAssignee() != null ? request.getNotifyAssignee() : event.getNotifyAssignee());
        event.setNotifyReporter(request.getNotifyReporter() != null ? request.getNotifyReporter() : event.getNotifyReporter());
        event.setNotifyWatchers(request.getNotifyWatchers() != null ? request.getNotifyWatchers() : event.getNotifyWatchers());
        event.setNotifyVoters(request.getNotifyVoters() != null ? request.getNotifyVoters() : event.getNotifyVoters());

        event = schemeEventRepository.save(event);
        log.info("Updated event: {} in scheme: {}", eventId, schemeId);

        return mapToEventResponse(event);
    }

    @Transactional
    public void deleteSchemeEvent(UUID schemeId, UUID eventId) {
        log.info("Deleting event {} from scheme: {}", eventId, schemeId);

        NotificationSchemeEvent event = schemeEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        if (!event.getSchemeId().equals(schemeId)) {
            throw new IllegalArgumentException("Event does not belong to this notification scheme");
        }

        schemeEventRepository.deleteById(eventId);
        log.info("Deleted event: {} from scheme: {}", eventId, schemeId);
    }

    private NotificationSchemeResponse mapToResponse(NotificationScheme scheme) {
        List<NotificationSchemeEventResponse> events = schemeEventRepository.findBySchemeId(scheme.getId())
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        return NotificationSchemeResponse.builder()
                .id(scheme.getId())
                .name(scheme.getName())
                .description(scheme.getDescription())
                .projectId(scheme.getProjectId())
                .createdBy(scheme.getCreatedBy())
                .isDefault(scheme.getIsDefault())
                .events(events)
                .createdAt(scheme.getCreatedAt())
                .updatedAt(scheme.getUpdatedAt())
                .build();
    }

    private NotificationSchemeEventResponse mapToEventResponse(NotificationSchemeEvent event) {
        return NotificationSchemeEventResponse.builder()
                .id(event.getId())
                .schemeId(event.getSchemeId())
                .eventType(event.getEventType())
                .recipientType(event.getRecipientType())
                .recipientId(event.getRecipientId())
                .recipientGroup(event.getRecipientGroup())
                .notificationTemplateId(event.getNotificationTemplateId())
                .enabled(event.getEnabled())
                .notifyAssignee(event.getNotifyAssignee())
                .notifyReporter(event.getNotifyReporter())
                .notifyWatchers(event.getNotifyWatchers())
                .notifyVoters(event.getNotifyVoters())
                .createdAt(event.getCreatedAt())
                .build();
    }
}