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

    @NotNull(message = "{validation.notification.userId.required}")
    private UUID userId;

    @NotBlank(message = "{validation.notification.type.required}")
    private String type;

    @NotBlank(message = "{validation.notification.title.required}")
    private String title;

    private String message;

    private String referenceType;

    private UUID referenceId;
}