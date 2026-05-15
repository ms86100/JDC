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

import java.time.LocalDateTime;
import java.util.*;

/**
 * Sprint Persister Handler
 * Handles sprint/iteration creation using real sprint service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SprintPersisterHandler {

    private final EntityStatusRepository entityStatusRepository;
    private final SprintServiceClient sprintServiceClient;
    private final ProjectServiceClient projectServiceClient;

    // Track created sprints for rollback
    private final List<String> createdSprintIds = new ArrayList<>();

    @Transactional(rollbackFor = Exception.class)
    public SprintPersistResult persistSprint(Map<String, Object> sprintData, UUID jobId) {
        SprintPersistResult result = new SprintPersistResult();

        try {
            String name = (String) sprintData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Sprint name is required");
            }

            String projectId = (String) sprintData.get("projectId");
            if (projectId == null) {
                throw new IllegalArgumentException("Project ID is required for sprint");
            }

            // Build create sprint request
            CreateSprintRequest request = buildCreateSprintRequest(sprintData, projectId);

            // Call real sprint service
            SprintResponse response = createSprintWithRetry(request);
            String sprintId = response.getId();

            // Track for potential rollback
            createdSprintIds.add(sprintId);

            // Add issues to sprint if provided
            List<String> issueKeys = (List<String>) sprintData.get("issueKeys");
            if (issueKeys != null && !issueKeys.isEmpty()) {
                addIssuesToSprint(sprintId, issueKeys);
            }

            // Update entity status
            updateEntityStatus(jobId, name, sprintId, "SPRINT", true);

            result.setSuccess(true);
            result.setSprintId(UUID.fromString(sprintId));
            result.setSprintName(name);

            log.info("Persisted sprint: {} with ID: {} for project {}", name, sprintId, projectId);

        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Sprint service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create sprint in service: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist sprint: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist sprint: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Create sprint with retry logic for transient failures.
     */
    private SprintResponse createSprintWithRetry(CreateSprintRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return sprintServiceClient.createSprint(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Sprint creation failed (attempt {}/{}), retrying in {}ms: {}",
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
        throw new MigrationException("Sprint creation failed after " + maxRetries + " attempts");
    }

    private CreateSprintRequest buildCreateSprintRequest(Map<String, Object> data, String projectId) {
        CreateSprintRequest.CreateSprintRequestBuilder builder = CreateSprintRequest.builder()
                .name((String) data.get("name"))
                .projectId(projectId)
                .goal((String) data.get("goal"));

        // Parse start date
        Object startDate = data.get("startDate");
        if (startDate != null) {
            try {
                builder.startDate(LocalDateTime.parse(startDate.toString()));
            } catch (Exception e) {
                log.debug("Could not parse start date: {}", startDate);
            }
        }

        // Parse end date
        Object endDate = data.get("endDate");
        if (endDate != null) {
            try {
                builder.endDate(LocalDateTime.parse(endDate.toString()));
            } catch (Exception e) {
                log.debug("Could not parse end date: {}", endDate);
            }
        }

        // Set duration if provided
        Integer durationDays = (Integer) data.get("durationDays");
        if (durationDays != null) {
            builder.durationDays(durationDays);
        }

        return builder.build();
    }

    private void addIssuesToSprint(String sprintId, List<String> issueKeys) {
        try {
            // Filter valid issue IDs (non-null, non-empty)
            List<String> validIssueIds = issueKeys.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .toList();

            if (!validIssueIds.isEmpty()) {
                sprintServiceClient.addIssuesToSprint(sprintId, validIssueIds);
                log.debug("Added {} issues to sprint {}", validIssueIds.size(), sprintId);
            }
        } catch (ServiceClientException e) {
            log.warn("Failed to add issues to sprint {}: {}", sprintId, e.getMessage());
        }
    }

    /**
     * Create sprint with start/end dates.
     */
    @Transactional(rollbackFor = Exception.class)
    public UUID createSprint(String name, String projectId, LocalDateTime startDate,
                            LocalDateTime endDate, String goal) {
        Map<String, Object> sprintData = new HashMap<>();
        sprintData.put("name", name);
        sprintData.put("projectId", projectId);
        sprintData.put("startDate", startDate != null ? startDate.toString() : null);
        sprintData.put("endDate", endDate != null ? endDate.toString() : null);
        sprintData.put("goal", goal);

        SprintPersistResult result = persistSprint(sprintData, null);
        return result.isSuccess() ? result.getSprintId() : null;
    }

    private void updateEntityStatus(UUID jobId, String sourceKey, String targetId,
                                    String type, boolean success) {
        if (jobId == null) return;

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
            status.setProcessedAt(LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Rollback created sprints on failure.
     */
    public void rollbackCreatedSprints() {
        log.info("Rolling back {} created sprints", createdSprintIds.size());
        for (String sprintId : createdSprintIds) {
            try {
                sprintServiceClient.deleteSprint(sprintId);
                log.debug("Rolled back sprint: {}", sprintId);
            } catch (Exception e) {
                log.error("Failed to rollback sprint {}: {}", sprintId, e.getMessage());
            }
        }
        createdSprintIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdSprintIds.clear();
    }

    public static class SprintPersistResult {
        private boolean success;
        private UUID sprintId;
        private String sprintName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getSprintId() { return sprintId; }
        public void setSprintId(UUID sprintId) { this.sprintId = sprintId; }
        public String getSprintName() { return sprintName; }
        public void setSprintName(String sprintName) { this.sprintName = sprintName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}