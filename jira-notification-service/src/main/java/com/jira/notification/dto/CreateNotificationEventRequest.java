package com.jira.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationEventRequest {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @Builder.Default
    private Boolean enabled = true;

    private String category;
    private String iconUrl;

    @Builder.Default
    private Boolean isSystemEvent = false;
}