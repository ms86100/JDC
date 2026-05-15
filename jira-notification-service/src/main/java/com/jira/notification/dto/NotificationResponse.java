package com.jira.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String message;
    private String referenceType;
    private UUID referenceId;
    private Boolean isRead;
    private OffsetDateTime createdAt;
}