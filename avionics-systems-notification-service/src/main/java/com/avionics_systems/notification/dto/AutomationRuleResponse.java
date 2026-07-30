package com.avionics_systems.notification.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID projectId;
    private UUID createdBy;
    private String triggerType;
    private Boolean enabled;
    private Boolean isSystemRule;
    private Integer executionCount;
    private OffsetDateTime lastExecutedAt;
    private String lastStatus;
    private Integer orderIndex;
    private List<AutomationTriggerResponse> triggers;
    private List<AutomationConditionResponse> conditions;
    private List<AutomationActionResponse> actions;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}