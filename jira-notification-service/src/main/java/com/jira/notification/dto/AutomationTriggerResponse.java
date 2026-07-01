package com.jira.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationTriggerResponse {

    private UUID id;
    private UUID ruleId;
    private String triggerType;
    private String triggerConfig;
    private Boolean enabled;
    private Integer orderIndex;
    private OffsetDateTime createdAt;
}