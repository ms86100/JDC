package com.jira.workflow.dto;

import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class ExecuteTransitionRequest {
    private UUID issueId;
    private UUID transitionId;
    private UUID projectId;
    private UUID userId;
    /** Target status when resolving transition by status (legacy) */
    private UUID statusId;
    private String comment;
    private UUID resolutionId;
    private Map<String, Object> screenInput;
    /** Client issue version for optimistic locking */
    private Long expectedVersion;
    /** Optional idempotency key — duplicate requests return cached success within TTL */
    private String idempotencyKey;
}
