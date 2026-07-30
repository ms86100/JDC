package com.avionics_systems.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for workflow transition triggers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTriggerResponse {

    private UUID id;

    private UUID transitionId;

    private String transitionName;

    private String workflowName;

    private String name;

    private String description;

    private String triggerType;

    private String triggerConfig;

    private Boolean isEnabled;

    private Integer executionOrder;

    private LocalDateTime lastTriggeredAt;

    private Integer triggerCount;

    private Integer cooldownSeconds;

    private Integer maxFireCount;

    private String conditions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}