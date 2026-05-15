package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Notification type is required")
    private String type;

    @NotBlank(message = "Title is required")
    private String title;

    private String message;

    private String referenceType;

    private UUID referenceId;
}