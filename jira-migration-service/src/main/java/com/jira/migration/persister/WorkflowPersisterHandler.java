package com.jira.migration.persister;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.exception.*;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.service.clients.*;
import com.jira.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Workflow Persister Handler
 * Handles workflow configuration persistence using real workflow service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowPersisterHandler {

    private final EntityStatusRepository entityStatusRepository;
    private final WorkflowServiceClient workflowServiceClient;

    // Track created workflows for rollback
    private final List<String> createdWorkflowIds = new ArrayList<>();

    @Transactional(rollbackFor = Exception.class)
    public WorkflowPersistResult persistWorkflow(Map<String, Object> workflowData, UUID jobId) {
        WorkflowPersistResult result = new WorkflowPersistResult();

        try {
            String workflowName = (String) workflowData.get("name");
            if (workflowName == null || workflowName.isBlank()) {
                throw new ValidationException("Workflow name is required", "WORKFLOW_NAME_REQUIRED", "name");
            }

            String projectId = (String) workflowData.get("projectId");

            // 1. Build workflow creation request
            CreateWorkflowRequest request = buildCreateWorkflowRequest(workflowData);

            // 2. Call real workflow service
            WorkflowResponse response = createWorkflowWithRetry(request);
            String workflowId = response.getId();

            // Track for potential rollback
            createdWorkflowIds.add(workflowId);

            // 3. Update entity status
            updateEntityStatus(jobId, workflowName, workflowId, "WORKFLOW", true);

            result.setSuccess(true);
            result.setWorkflowId(UUID.fromString(workflowId));
            result.setWorkflowName(workflowName);

            log.info("Persisted workflow: {} with ID: {}", workflowName, workflowId);

        } catch (ValidationException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Workflow service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create workflow in service: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist workflow: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist workflow: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Create workflow with retry logic for transient failures.
     */
    private WorkflowResponse createWorkflowWithRetry(CreateWorkflowRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return workflowServiceClient.createWorkflow(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Workflow creation failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, maxRetries, baseDelayMs * attempt, e.getMessage());
                    try {
                        Thread.sleep(baseDelayMs * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new MigrationException("Workflow creation failed after " + maxRetries + " attempts");
    }

    private CreateWorkflowRequest buildCreateWorkflowRequest(Map<String, Object> data) {
        CreateWorkflowRequest.CreateWorkflowRequestBuilder builder = CreateWorkflowRequest.builder()
                .name((String) data.get("name"))
                .description((String) data.get("description"))
                .isDefault((Boolean) data.getOrDefault("isDefault", false));

        // Build workflow steps
        List<Map<String, Object>> statuses = (List<Map<String, Object>>) data.get("statuses");
        if (statuses != null && !statuses.isEmpty()) {
            List<CreateWorkflowRequest.WorkflowStepRequest> steps = new ArrayList<>();
            int order = 0;
            for (Map<String, Object> status : statuses) {
                CreateWorkflowRequest.WorkflowStepRequest step = new CreateWorkflowRequest.WorkflowStepRequest();
                step.setName((String) status.get("name"));
                step.setOrder(order++);
                step.setStatusCategory((String) status.get("statusCategory"));
                step.setIcon((String) status.get("icon"));
                step.setColor((String) status.get("color"));
                steps.add(step);
            }
            builder.steps(steps);
        }

        // Build workflow transitions
        List<Map<String, Object>> transitions = (List<Map<String, Object>>) data.get("transitions");
        if (transitions != null && !transitions.isEmpty()) {
            List<CreateWorkflowRequest.WorkflowTransitionRequest> transitionReqs = new ArrayList<>();
            for (Map<String, Object> transition : transitions) {
                CreateWorkflowRequest.WorkflowTransitionRequest transReq =
                        new CreateWorkflowRequest.WorkflowTransitionRequest();
                transReq.setName((String) transition.get("name"));
                transReq.setFromStep((String) transition.get("fromStatus"));
                transReq.setToStep((String) transition.get("toStatus"));
                transReq.setTrigger((String) transition.getOrDefault("trigger", "MANUAL"));
                transReq.setConditions((List<String>) transition.get("conditions"));
                transReq.setPostFunctions((List<String>) transition.get("postFunctions"));
                transReq.setValidators((List<String>) transition.get("validators"));
                transitionReqs.add(transReq);
            }
            builder.transitions(transitionReqs);
        }

        return builder.build();
    }

    /**
     * Validate workflow transition against statuses.
     */
    public void validateWorkflow(Map<String, Object> workflowData) {
        List<String> statusNames = new ArrayList<>();

        List<Map<String, Object>> statuses = (List<Map<String, Object>>) workflowData.get("statuses");
        if (statuses != null) {
            for (Map<String, Object> status : statuses) {
                statusNames.add((String) status.get("name"));
            }
        }

        List<Map<String, Object>> transitions = (List<Map<String, Object>>) workflowData.get("transitions");
        if (transitions != null) {
            for (Map<String, Object> transition : transitions) {
                String fromStatus = (String) transition.get("fromStatus");
                String toStatus = (String) transition.get("toStatus");

                if (!statusNames.contains(fromStatus)) {
                    throw new ValidationException("Transition references unknown status: " + fromStatus,
                            "INVALID_TRANSITION", "fromStatus");
                }
                if (!statusNames.contains(toStatus)) {
                    throw new ValidationException("Transition references unknown status: " + toStatus,
                            "INVALID_TRANSITION", "toStatus");
                }
            }
        }
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId,
                                    String type, boolean success) {
        try {
            EntityStatus status = entityStatusRepository
                    .findByJobIdAndEntityTypeAndSourceIdentifier(jobId, type, sourceKey)
                    .orElse(EntityStatus.builder()
                            .jobId(jobId)
                            .entityType(type)
                            .sourceIdentifier(sourceKey)
                            .build());

            status.setTargetId(targetId);
            status.setStatus(success ? "SUCCESS" : "FAILED");
            status.setProcessedAt(java.time.LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Rollback created workflows on failure.
     */
    public void rollbackCreatedWorkflows() {
        log.info("Rolling back {} created workflows", createdWorkflowIds.size());
        for (String workflowId : createdWorkflowIds) {
            try {
                workflowServiceClient.deleteWorkflow(workflowId);
                log.debug("Rolled back workflow: {}", workflowId);
            } catch (Exception e) {
                log.error("Failed to rollback workflow {}: {}", workflowId, e.getMessage());
            }
        }
        createdWorkflowIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdWorkflowIds.clear();
    }

    public static class WorkflowPersistResult {
        private boolean success;
        private UUID workflowId;
        private String workflowName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getWorkflowId() { return workflowId; }
        public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}