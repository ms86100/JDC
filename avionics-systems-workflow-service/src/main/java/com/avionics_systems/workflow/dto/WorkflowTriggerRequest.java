package com.avionics_systems.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating or updating a workflow transition trigger.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTriggerRequest {

    @NotNull(message = "{validation.transition.id.required}")
    private UUID transitionId;

    private String name;

    private String description;

    @NotBlank(message = "{validation.trigger.type.required}")
    private String triggerType;

    /**
     * JSON configuration for the trigger.
     * Structure varies by trigger type:
     *
     * FIELD_CHANGE: {"fieldName": "status", "operator": "CHANGED_TO", "value": "Done"}
     * COMMENT_ADDED: {"pattern": ".*build.*", "regex": true}
     * DATE_BASED: {"dateField": "dueDate", "offsetMinutes": 0}
     * EXTERNAL_WEBHOOK: {"webhookId": "uuid", "secret": "..."}
     */
    private Map<String, Object> triggerConfig;

    @Builder.Default
    private Boolean isEnabled = true;

    @Builder.Default
    private Integer executionOrder = 0;

    @Builder.Default
    private Integer cooldownSeconds = 60;

    @Builder.Default
    private Integer maxFireCount = 0;

    /**
     * Optional conditions that must be met for the trigger to fire.
     * Format: [{"field": "status", "operator": "EQUALS", "value": "In Progress"}]
     */
    private java.util.List<Map<String, Object>> conditions;
}