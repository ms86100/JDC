package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDispatchRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    private UUID issueId;

    private UUID projectId;

    private UUID actorUserId;

    private String title;

    private String message;
}
