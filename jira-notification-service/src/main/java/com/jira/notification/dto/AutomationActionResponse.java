package com.jira.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationActionResponse {

    private UUID id;
    private UUID ruleId;
    private String actionType;
    private String actionConfig;
    private Boolean enabled;
    private Integer orderIndex;
    private String failureHandling;
    private OffsetDateTime createdAt;
}