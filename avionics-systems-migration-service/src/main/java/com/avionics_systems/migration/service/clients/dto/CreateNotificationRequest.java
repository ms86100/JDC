package com.avionics_systems.migration.service.clients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a Notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

    @NotNull(message = "Recipient ID is required")
    private String recipientId;

    @NotBlank(message = "Notification type is required")
    private String notificationType;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String body;
    private String issueId;
    private String projectId;
    private String senderId;
    private LocalDateTime sentAt;
    private boolean emailNotification;
    private boolean inAppNotification;
    private String priority;
    private String actionUrl;
    private String icon;
}