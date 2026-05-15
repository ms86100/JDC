package com.jira.sprint.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilterSubscriptionResponse {
    private UUID id;
    private UUID userId;
    private String filterName;
    private String jqlQuery;
    private SubscriptionFrequency frequency;
    private Boolean isActive;
    private Boolean emailNotification;
    private LocalDateTime lastNotified;
    private LocalDateTime createdAt;
}