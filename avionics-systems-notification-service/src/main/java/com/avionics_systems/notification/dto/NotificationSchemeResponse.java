package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSchemeResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private UUID createdBy;
    private Boolean isDefault;
    private List<NotificationSchemeEventResponse> events;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}