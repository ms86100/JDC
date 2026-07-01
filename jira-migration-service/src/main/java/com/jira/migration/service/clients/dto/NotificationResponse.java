package com.jira.migration.service.clients.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Notification operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationResponse {

    @EqualsAndHashCode.Include
    private String id;

    private String recipientId;
    private String recipientEmail;
    private String recipientDisplayName;
    private String notificationType;
    private String subject;
    private String body;
    private String issueId;
    private String projectId;
    private String senderId;
    private LocalDateTime sentAt;
    private boolean read;
    private LocalDateTime readAt;
    private boolean emailNotification;
    private boolean inAppNotification;
    private String priority;
    private String actionUrl;
    private String icon;
    private boolean success;
    private String errorMessage;
}