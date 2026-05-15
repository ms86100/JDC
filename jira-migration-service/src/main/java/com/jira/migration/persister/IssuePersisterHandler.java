package com.jira.migration.persister;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.exception.*;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.service.clients.*;
import com.jira.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Issue Persister Handler
 * Handles issue creation with Epic/Story/Subtask hierarchy support using real service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IssuePersisterHandler {

    private final ProjectMappingRepository projectMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final IssueServiceClient issueServiceClient;
    private final IssueLinkServiceClient issueLinkServiceClient;

    // Track created issues for rollback
    private final List<String> createdIssueIds = new ArrayList<>();

    // Issue type hierarchy
    private static final Map<String, String> PARENT_TYPE_MAP = Map.of(
            "Epic", null,
            "Story", "Epic",
            "Task", null,
            "Bug", null,
            "Subtask", "Story"
    );

    @Transactional(rollbackFor = Exception.class)
    public IssuePersisterResult persistIssue(Map<String, Object> issueData, UUID jobId) {
        IssuePersisterResult result = new IssuePersisterResult();

        try {
            // 1. Extract and validate project
            String projectKey = (String) issueData.get("projectKey");
            String projectId = (String) issueData.get("projectId");

            if (projectId == null) {
                if (projectKey == null) {
                    throw new ValidationException("Project key or ID is required", "PROJECT_KEY_REQUIRED", "projectKey");
                }
                // Lookup project ID from mapping
                ProjectMapping projectMapping = projectMappingRepository
                        .findByJobIdAndSourceKey(jobId, projectKey)
                        .orElseThrow(() -> new EntityNotFoundException("Project", projectKey));
                projectId = projectMapping.getTargetId().toString();
            }

            // 2. Extract and validate issue type
            String issueType = (String) issueData.get("issueType");
            if (issueType == null || issueType.isBlank()) {
                throw new ValidationException("Issue type is required", "ISSUE_TYPE_REQUIRED", "issueType");
            }

            // 3. Validate parent relationship
            String parentIssueKey = (String) issueData.get("parentIssueKey");
            validateParentRelationship(issueType, parentIssueKey, jobId);

            // 4. Build create issue request
            CreateIssueRequest request = buildCreateIssueRequest(issueData, projectId);

            // 5. Call real issue service
            IssueResponse response = createIssueWithRetry(request);
            String issueId = response.getId();
            String issueKey = response.getKey();

            // Track for potential rollback
            createdIssueIds.add(issueId);

            // 6. Handle Epic link (link story to epic)
            if ("Story".equalsIgnoreCase(issueType) && issueData.containsKey("epicLink")) {
                String epicKey = (String) issueData.get("epicLink");
                String epicId = resolveIssueId(epicKey, jobId);
                if (epicId != null) {
                    linkStoryToEpic(issueId, epicId);
                }
            }

            // 7. Add labels if present
            List<String> labels = (List<String>) issueData.get("labels");
            if (labels != null && !labels.isEmpty()) {
                addLabels(issueId, labels);
            }

            // 8. Update entity status
            updateEntityStatus(jobId, issueKey, issueId, "ISSUE", true);

            result.setSuccess(true);
            result.setIssueId(UUID.fromString(issueId));
            result.setIssueKey(issueKey);

            log.info("Persisted issue: {} ({}) with ID: {}", issueKey, issueType, issueId);

        } catch (ValidationException e) {
            result.setSuccess(false);
            result.setErrorCode(e.getErrorCode());
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorCode("ISSUE_SERVICE_ERROR");
            result.setErrorMessage(e.getMessage());
            log.error("Issue service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create issue in service: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorCode("ISSUE_PERSIST_ERROR");
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist issue: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist issue: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Create issue with retry logic for transient failures.
     */
    private IssueResponse createIssueWithRetry(CreateIssueRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return issueServiceClient.createIssue(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Issue creation failed (attempt {}/{}), retrying in {}ms: {}",
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
        throw new MigrationException("Issue creation failed after " + maxRetries + " attempts");
    }

    private CreateIssueRequest buildCreateIssueRequest(Map<String, Object> data, String projectId) {
        CreateIssueRequest.CreateIssueRequestBuilder builder = CreateIssueRequest.builder()
                .projectId(projectId)
                .issueType((String) data.get("issueType"))
                .summary((String) data.getOrDefault("summary", ""))
                .description((String) data.get("description"))
                .status((String) data.getOrDefault("status", "Open"))
                .priority((String) data.get("priority"))
                .assigneeId((String) data.get("assigneeId"))
                .reporterId((String) data.get("reporterId"))
                .originalIssueKey((String) data.get("issueKey"));

        // Handle labels
        Object labelsObj = data.get("labels");
        if (labelsObj instanceof List) {
            builder.labels((List<String>) labelsObj);
        }

        // Handle parent
        String parentId = (String) data.get("parentId");
        String parentIssueKey = (String) data.get("parentIssueKey");
        if (parentId != null) {
            builder.parentId(parentId);
        }

        // Handle sprint
        String sprintId = (String) data.get("sprintId");
        if (sprintId != null) {
            builder.sprintId(sprintId);
        }

        // Handle epic link
        String epicId = (String) data.get("epicId");
        if (epicId != null) {
            builder.epicId(epicId);
        }

        // Handle due date
        Object dueDate = data.get("dueDate");
        if (dueDate != null) {
            try {
                builder.dueDate(LocalDateTime.parse(dueDate.toString()));
            } catch (Exception e) {
                log.debug("Could not parse due date: {}", dueDate);
            }
        }

        // Handle story points
        Object storyPoints = data.get("storyPoints");
        if (storyPoints != null) {
            if (storyPoints instanceof Number) {
                builder.storyPoints(((Number) storyPoints).doubleValue());
            }
        }

        // Handle custom fields
        Object customFields = data.get("customFields");
        if (customFields instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cf = (Map<String, Object>) customFields;
            builder.customFields(cf);
        }

        return builder.build();
    }

    /**
     * Validate parent-child relationship based on issue type hierarchy.
     */
    private void validateParentRelationship(String issueType, String parentKey, UUID jobId) {
        String expectedParentType = PARENT_TYPE_MAP.get(issueType);

        if (expectedParentType == null && parentKey != null) {
            throw new ValidationException(
                    issueType + " cannot have a parent issue",
                    "INVALID_PARENT_TYPE",
                    "parentIssueKey"
            );
        }

        if (expectedParentType != null && parentKey != null) {
            log.debug("Validating parent relationship: {} -> {} (expected parent type: {})",
                    issueType, parentKey, expectedParentType);
        }
    }

    private void linkStoryToEpic(String storyId, String epicId) {
        try {
            issueLinkServiceClient.linkStoryToEpic(storyId, epicId);
            log.debug("Linked story {} to epic {}", storyId, epicId);
        } catch (ServiceClientException e) {
            log.warn("Failed to link story {} to epic {}: {}", storyId, epicId, e.getMessage());
        }
    }

    private void addLabels(String issueId, List<String> labels) {
        // Labels are added during issue creation via customFields
        log.debug("Labels {} will be added to issue {} during creation", labels.size(), issueId);
    }

    private String resolveIssueId(String issueKey, UUID jobId) {
        // Use getIssue to resolve issue key - simplified implementation
        try {
            IssueResponse issue = issueServiceClient.getIssue(issueKey);
            return issue != null ? issue.getId() : null;
        } catch (Exception e) {
            log.debug("Could not resolve issue ID for key {}: {}", issueKey, e.getMessage());
            return null;
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
            status.setProcessedAt(LocalDateTime.now());

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    /**
     * Batch persist issues with proper ordering.
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchPersistResult batchPersistIssues(List<Map<String, Object>> issues, UUID jobId) {
        BatchPersistResult result = new BatchPersistResult();
        List<IssuePersisterResult> successList = new ArrayList<>();
        List<IssuePersisterResult> failureList = new ArrayList<>();

        // Sort issues by hierarchy (Epics -> Stories -> Tasks -> Subtasks)
        List<Map<String, Object>> sorted = sortByHierarchy(issues);

        for (Map<String, Object> issue : sorted) {
            try {
                IssuePersisterResult persistResult = persistIssue(issue, jobId);
                if (persistResult.isSuccess()) {
                    successList.add(persistResult);
                } else {
                    failureList.add(persistResult);
                }
            } catch (Exception e) {
                IssuePersisterResult failure = new IssuePersisterResult();
                failure.setSuccess(false);
                failure.setErrorMessage(e.getMessage());
                failureList.add(failure);
                log.error("Failed to persist issue: {}", e.getMessage());
            }
        }

        result.setSuccessCount(successList.size());
        result.setFailureCount(failureList.size());
        result.setSuccesses(successList);
        result.setFailures(failureList);

        if (!failureList.isEmpty()) {
            result.setHasFailures(true);
            result.setErrorSummary(failureList.size() + " issues failed to import");
        }

        log.info("Batch issue persist completed: {} succeeded, {} failed",
                successList.size(), failureList.size());

        return result;
    }

    private List<Map<String, Object>> sortByHierarchy(List<Map<String, Object>> issues) {
        List<Map<String, Object>> sorted = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        // Phase 1: Process Epics first (no parent allowed)
        for (Map<String, Object> issue : issues) {
            if ("Epic".equalsIgnoreCase((String) issue.getOrDefault("issueType", ""))) {
                sorted.add(issue);
                processed.add((String) issue.get("issueKey"));
            }
        }

        // Phase 2: Process Stories (can have Epic parent)
        for (Map<String, Object> issue : issues) {
            String key = (String) issue.get("issueKey");
            if (!processed.contains(key) && "Story".equalsIgnoreCase((String) issue.getOrDefault("issueType", ""))) {
                String parentKey = (String) issue.get("parentIssueKey");
                if (parentKey == null || processed.contains(parentKey)) {
                    sorted.add(issue);
                    processed.add(key);
                }
            }
        }

        // Phase 3: Process Tasks and Bugs
        for (Map<String, Object> issue : issues) {
            String key = (String) issue.get("issueKey");
            if (!processed.contains(key)) {
                String type = (String) issue.getOrDefault("issueType", "");
                if ("Task".equalsIgnoreCase(type) || "Bug".equalsIgnoreCase(type)) {
                    sorted.add(issue);
                    processed.add(key);
                }
            }
        }

        // Phase 4: Process Subtasks (must have parent)
        for (Map<String, Object> issue : issues) {
            String key = (String) issue.get("issueKey");
            if (!processed.contains(key) && "Subtask".equalsIgnoreCase((String) issue.getOrDefault("issueType", ""))) {
                String parentKey = (String) issue.get("parentIssueKey");
                if (parentKey != null && processed.contains(parentKey)) {
                    sorted.add(issue);
                    processed.add(key);
                }
            }
        }

        // Phase 5: Add any remaining (in original order)
        for (Map<String, Object> issue : issues) {
            String key = (String) issue.get("issueKey");
            if (!processed.contains(key)) {
                sorted.add(issue);
                processed.add(key);
            }
        }

        return sorted;
    }

    /**
     * Rollback created issues on failure.
     */
    public void rollbackCreatedIssues() {
        log.info("Rolling back {} created issues", createdIssueIds.size());
        for (String issueId : createdIssueIds) {
            try {
                issueServiceClient.deleteIssue(issueId);
                log.debug("Rolled back issue: {}", issueId);
            } catch (Exception e) {
                log.error("Failed to rollback issue {}: {}", issueId, e.getMessage());
            }
        }
        createdIssueIds.clear();
    }

    /**
     * Clear rollback tracking.
     */
    public void clearRollbackTracking() {
        createdIssueIds.clear();
    }

    public static class IssuePersisterResult {
        private boolean success;
        private UUID issueId;
        private String issueKey;
        private String errorCode;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getIssueId() { return issueId; }
        public void setIssueId(UUID issueId) { this.issueId = issueId; }
        public String getIssueKey() { return issueKey; }
        public void setIssueKey(String issueKey) { this.issueKey = issueKey; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class BatchPersistResult {
        private int successCount;
        private int failureCount;
        private boolean hasFailures;
        private String errorSummary;
        private List<IssuePersisterResult> successes;
        private List<IssuePersisterResult> failures;

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public boolean isHasFailures() { return hasFailures; }
        public void setHasFailures(boolean hasFailures) { this.hasFailures = hasFailures; }
        public String getErrorSummary() { return errorSummary; }
        public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
        public List<IssuePersisterResult> getSuccesses() { return successes; }
        public void setSuccesses(List<IssuePersisterResult> successes) { this.successes = successes; }
        public List<IssuePersisterResult> getFailures() { return failures; }
        public void setFailures(List<IssuePersisterResult> failures) { this.failures = failures; }
    }
}