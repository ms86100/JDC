package com.jira.notification.service;

import com.jira.notification.dto.*;
import com.jira.notification.entity.Notification;
import com.jira.notification.entity.NotificationPreference;
import com.jira.notification.exception.ResourceNotFoundException;
import com.jira.notification.repository.NotificationPreferenceRepository;
import com.jira.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Transactional
    public NotificationResponse createNotification(NotificationEvent event) {
        log.info("Creating notification for user: {} - type: {}", event.getUserId(), event.getType());

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .referenceType(event.getReferenceType())
                .referenceId(event.getReferenceId())
                .isRead(false)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Created notification with id: {}", notification.getId());

        // Send email notification if enabled
        sendEmailNotification(notification);

        return mapToResponse(notification);
    }

    private void sendEmailNotification(Notification notification) {
        if (!emailEnabled) {
            log.debug("Email notifications disabled, skipping email for {}", notification.getId());
            return;
        }

        try {
            Map<String, Object> notificationData = Map.of(
                    "issueKey", notification.getReferenceId() != null ? notification.getReferenceId().toString() : "",
                    "title", notification.getTitle(),
                    "message", notification.getMessage(),
                    "projectKey", ""
            );

            switch (notification.getType()) {
                case "ISSUE_ASSIGNED":
                    emailService.sendIssueAssignedEmail(notification.getUserId(), notificationData);
                    break;
                case "ISSUE_COMMENTED":
                    emailService.sendIssueCommentedEmail(notification.getUserId(), notificationData);
                    break;
                case "SPRINT_STARTED":
                    emailService.sendSprintStartedEmail(notification.getUserId(), notificationData);
                    break;
                case "SPRINT_COMPLETED":
                    emailService.sendSprintCompletedEmail(notification.getUserId(), notificationData);
                    break;
                default:
                    emailService.sendBulkNotificationEmail(
                            notification.getUserId(),
                            notification.getTitle(),
                            notification.getMessage()
                    );
            }
        } catch (Exception e) {
            log.error("Failed to send email notification for {}: {}", notification.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Boolean read, int page, int size) {
        log.debug("Fetching notifications for user: {}, read: {}", userId, read);

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications;

        if (read != null) {
            notifications = notificationRepository.findByUserIdAndIsRead(userId, read, pageable);
        } else {
            notifications = notificationRepository.findByUserId(userId, pageable);
        }

        return notifications.map(this::mapToResponse);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        log.info("Marking notification as read: {}", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read", updated);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        log.debug("Getting unread notification count for user: {}", userId);
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(UUID userId) {
        log.debug("Fetching notification preferences for user: {}", userId);

        List<NotificationPreference> preferences = preferenceRepository.findByUserId(userId);

        return preferences.stream()
                .map(this::mapToPreferenceResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(UUID userId, NotificationPreferencesRequest request) {
        log.info("Updating notification preferences for user: {}", userId);

        // Delete existing preferences
        preferenceRepository.deleteAllByUserId(userId);

        // Create new preferences
        if (request.getPreferences() != null) {
            List<NotificationPreference> preferences = request.getPreferences().entrySet().stream()
                    .map(entry -> NotificationPreference.builder()
                            .userId(userId)
                            .notificationType(entry.getKey())
                            .enabled(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            preferenceRepository.saveAll(preferences);
        }

        return getPreferences(userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        log.info("Deleting notification: {}", notificationId);

        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }

        notificationRepository.deleteById(notificationId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private NotificationPreferenceResponse mapToPreferenceResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .userId(preference.getUserId())
                .notificationType(preference.getNotificationType())
                .enabled(preference.getEnabled())
                .build();
    }
}