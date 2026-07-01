package com.jira.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventResponse {

    private UUID id;
    private String eventType;
    private String name;
    private String description;
    private Boolean enabled;
    private String category;
    private String iconUrl;
    private Boolean isSystemEvent;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}