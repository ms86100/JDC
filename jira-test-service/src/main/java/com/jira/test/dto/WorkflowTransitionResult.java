package com.jira.test.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result of a workflow transition attempt through the WorkflowBridgeService.
 * Captures whether the transition succeeded or failed, with error details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionResult {

    private boolean success;
    private UUID entityId;
    private String fromStatus;
    private String toStatus;
    private String errorMessage;

    public static WorkflowTransitionResult success(UUID entityId, String from, String to) {
        return WorkflowTransitionResult.builder()
                .success(true)
                .entityId(entityId)
                .fromStatus(from)
                .toStatus(to)
                .build();
    }

    public static WorkflowTransitionResult failure(String error) {
        return WorkflowTransitionResult.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}
