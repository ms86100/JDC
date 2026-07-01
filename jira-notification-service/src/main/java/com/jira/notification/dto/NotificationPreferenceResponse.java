package com.jira.notification.dto;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {

    private UUID userId;
    private String notificationType;
    private Boolean enabled;
}