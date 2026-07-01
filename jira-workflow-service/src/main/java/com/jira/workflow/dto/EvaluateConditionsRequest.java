package com.jira.workflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for evaluating workflow conditions.
 * Used by external services (like jira-issue-service) to evaluate conditions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluateConditionsRequest {

    /**
     * The transition ID to evaluate conditions for
     */
    @NotNull(message = "Transition ID is required")
    private UUID transitionId;

    /**
     * Current user performing the transition
     */
    private UUID userId;

    /**
     * Groups the current user belongs to
     */
    private Set<String> userGroups;

    /**
     * Issue ID being transitioned
     */
    private UUID issueId;

    /**
     * Project ID for permission checks
     */
    private UUID projectId;

    /**
     * Issue fields as a map (field name -> value)
     */
    private Map<String, Object> fields;

    /**
     * Previous status ID before transition
     */
    private UUID previousStatusId;

    /**
     * Current status ID
     */
    private UUID currentStatusId;

    /**
     * Reporter ID of the issue
     */
    private UUID reporterId;

    /**
     * Assignee ID of the issue
     */
    private UUID assigneeId;

    /**
     * Screen input values (fields being changed in this transition)
     */
    private Map<String, Object> screenInput;
}