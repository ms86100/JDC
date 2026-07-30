package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSchemeEventResponse {

    private UUID id;
    private UUID schemeId;
    private String eventType;
    private String recipientType;
    private UUID recipientId;
    private String recipientGroup;
    private UUID notificationTemplateId;
    private Boolean enabled;
    private Boolean notifyAssignee;
    private Boolean notifyReporter;
    private Boolean notifyWatchers;
    private Boolean notifyVoters;
    private OffsetDateTime createdAt;
}