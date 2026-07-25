package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationSchemeEventRequest {

    @NotBlank(message = "{validation.scheme.event.type.required}")
    private String eventType;

    @NotBlank(message = "{validation.scheme.event.recipientType.required}")
    private String recipientType;

    private UUID recipientId;
    private String recipientGroup;
    private UUID notificationTemplateId;

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean notifyAssignee = false;

    @Builder.Default
    private Boolean notifyReporter = false;

    @Builder.Default
    private Boolean notifyWatchers = false;

    @Builder.Default
    private Boolean notifyVoters = false;
}