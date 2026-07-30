package com.avionics_systems.migration.persister;

import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.ProjectMapping;
import com.avionics_systems.migration.exception.*;
import com.avionics_systems.migration.repository.EntityStatusRepository;
import com.avionics_systems.migration.repository.ProjectMappingRepository;
import com.avionics_systems.migration.service.MigrationWorkflowStatusApplier;
import com.avionics_systems.migration.service.UserDirectoryMappingService;
import com.avionics_systems.migration.service.clients.*;
import com.avionics_systems.migration.service.clients.dto.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Issue Persister Handler
 * Handles issue creation with Epic/Story/Subtask hierarchy support using real service calls.
 */
@Component
@Slf4j
public class IssuePersisterHandler {

    private final ProjectMappingRepository projectMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final CustomFieldPersisterHandler customFieldPersisterHandler;
    private final Map<String, String> parentTypeMap;
    private IssueServiceClient issueServiceClient;
    private IssueLinkServiceClient issueLinkServiceClient;
    private MigrationWorkflowStatusApplier migrationWorkflowStatusApplier;
    private UserDirectoryMappingService userDirectoryMappingService;

    // Track created issues for rollback
    private final List<String> createdIssueIds = new ArrayList<>();

    public IssuePersisterHandler(
            ProjectMappingRepository projectMappingRepository,
            EntityStatusRepository entityStatusRepository,
            CustomFieldPersisterHandler customFieldPersisterHandler,
            @Value("${app.issue.hierarchy.epic-parent:}") String epicParent,
            @Value("${app.issue.hierarchy.story-parent:Epic}") String storyParent,
            @Value("${app.issue.hierarchy.task-parent:}") String taskParent,
            @Value("${app.issue.hierarchy.bug-parent:}") String bugParent,
            @Value("${app.issue.hierarchy.subtask-parent:Story}") String subtaskParent) {
        this.projectMappingRepository = projectMappingRepository;
        this.entityStatusRepository = entityStatusRepository;
        this.customFieldPersisterHandler = customFieldPersisterHandler;

        Map<String, String> temp = new HashMap<>();
        temp.put("Epic", epicParent.isEmpty() ? null : epicParent);
        temp.put("Story", storyParent.isEmpty() ? null : storyParent);
        temp.put("Task", taskParent.isEmpty() ? null : taskParent);
        temp.put("Bug", bugParent.isEmpty() ? null : bugParent);
        temp.put("Subtask", subtaskParent.isEmpty() ? null : subtaskParent);
        this.parentTypeMap = Collections.unmodifiableMap(temp);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setServiceClients(
            IssueServiceClient issueServiceClient,
            IssueLinkServiceClient issueLinkServiceClient,
            MigrationWorkflowStatusApplier migrationWorkflowStatusApplier,
            UserDirectoryMappingService userDirectoryMappingService) {
        this.issueServiceClient = issueServiceClient;
        this.issueLinkServiceClient = issueLinkServiceClient;
        this.migrationWorkflowStatusApplier = migrationWorkflowStatusApplier;
        this.userDirectoryMappingService = userDirectoryMappingService;
    }

    // parentTypeMap is now injected via @Value constructor params above

    @Transactional(rollbackFor = Exception.class)
    public IssuePersisterResult persistIssue(Map<String, Object> issueData, UUID jobId) {
        IssuePersisterResult result = new IssuePersisterResult();
        result.setRowNumber(extractRowNumber(issueData));
        String sourceIssueKey = stringVal(issueData.get("issueKey"));
        if (sourceIssueKey == null) {
            sourceIssueKey = stringVal(issueData.get("issue_key"));
        }
        result.setSourceIssueKey(sourceIssueKey);

        try {
            // 1. Extract and validate project (handle both project_key and projectKey)
            String projectKey = (String) issueData.getOrDefault("projectKey", issueData.get("project_key"));
            String projectId = (String) issueData.getOrDefault("projectId", issueData.get("project_id"));

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

            // 2. Extract and validate issue type (handle both issue_type and issueType)
            String issueType = (String) issueData.getOrDefault("issueType", issueData.get("issue_type"));
            if (issueType == null || issueType.isBlank()) {
                throw new ValidationException("Issue type is required", "ISSUE_TYPE_REQUIRED", "issueType");
            }

            // 3. Validate parent relationship
            String parentIssueKey = (String) issueData.get("parentIssueKey");
            validateParentRelationship(issueType, parentIssueKey, jobId);

            // 4. Resolve people fields and build create issue request
            enrichPeopleFields(issueData, jobId);
            CreateIssueRequest request = buildCreateIssueRequest(issueData, projectId);

            // 5. Call real issue service
            IssueResponse response = createIssueWithRetry(request);
            String issueId = response.getId();
            String issueKey = resolveCreatedIssueKey(response, issueId);
            if (issueKey == null || issueKey.isBlank()) {
                log.warn("Created issue {} has no issueKey in response — UI target key may be missing", issueId);
            }

            String sourceStatus = (String) issueData.getOrDefault("status", issueData.get("issue_status"));
            if (migrationWorkflowStatusApplier != null && sourceStatus != null && !sourceStatus.isBlank()) {
                migrationWorkflowStatusApplier.applyImportedStatus(jobId, response, sourceStatus);
            }

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
            Object labelsRaw = issueData.getOrDefault("labels", issueData.get("label"));
            List<String> labels = null;
            if (labelsRaw instanceof List<?> labelList) {
                labels = new ArrayList<>();
                for (Object o : labelList) {
                    if (o != null && !o.toString().isBlank()) {
                        labels.add(o.toString().trim());
                    }
                }
            } else if (labelsRaw instanceof String labelsStr && !labelsStr.isBlank()) {
                labels = Arrays.stream(labelsStr.split("[,;]"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
            }
            if (labels != null && !labels.isEmpty()) {
                addLabels(issueId, labels);
            }

            // 8. Persist custom field values to migration field store
            Object customFields = issueData.getOrDefault("customFields", issueData.get("custom_fields"));
            if (customFields instanceof Map<?, ?> cfMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> values = (Map<String, Object>) cfMap;
                customFieldPersisterHandler.persistCustomFieldValues(
                        UUID.fromString(issueId), values, jobId);
            }

            // 9. Update entity status (lookup by source CSV key, not target PXX-N)
            String entitySourceKey = sourceIssueKey != null ? sourceIssueKey : issueKey;
            updateEntityStatus(jobId, entitySourceKey, issueId, "ISSUE", true);

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

    private void enrichPeopleFields(Map<String, Object> data, UUID jobId) {
        if (userDirectoryMappingService == null || jobId == null) {
            return;
        }
        putResolvedUserId(data, "assignee", "assigneeId", jobId);
        putResolvedUserId(data, "reporter", "reporterId", jobId);
    }

    private void putResolvedUserId(Map<String, Object> data, String sourceKey, String targetKey, UUID jobId) {
        Object raw = data.get(sourceKey);
        if (raw == null || raw.toString().isBlank()) {
            return;
        }
        String source = raw.toString().trim();
        if (source.length() == 36 && source.contains("-")) {
            data.put(targetKey, source);
            return;
        }
        try {
            UUID target = userDirectoryMappingService.resolveToTargetUserId(source, jobId);
            if (target != null) {
                data.put(targetKey, target.toString());
            } else {
                // Do not pass Legacy usernames as UUIDs to issue-service.
                data.remove(sourceKey);
                data.remove(targetKey);
                log.info("Assignee/reporter '{}' not resolved — issue will import without {}", source, targetKey);
            }
        } catch (CallNotPermittedException e) {
            data.remove(sourceKey);
            data.remove(targetKey);
            log.warn("User-service circuit open — continuing without {} for {}", targetKey, source);
        } catch (Exception e) {
            data.remove(sourceKey);
            data.remove(targetKey);
            log.warn("User resolution failed for {} — continuing without {}: {}", source, targetKey, e.getMessage());
        }
    }

    private String resolveCreatedIssueKey(IssueResponse response, String issueId) {
        if (response.getKey() != null && !response.getKey().isBlank()) {
            return response.getKey();
        }
        if (issueId != null) {
            try {
                IssueResponse fetched = issueServiceClient.getIssue(issueId);
                if (fetched.getKey() != null && !fetched.getKey().isBlank()) {
                    return fetched.getKey();
                }
            } catch (Exception e) {
                log.debug("Could not fetch issue key for {}: {}", issueId, e.getMessage());
            }
        }
        return null;
    }

    private Long parseDurationSeconds(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) {
                continue;
            }
            String s = c.toString().trim();
            if (s.isBlank()) {
                continue;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
                // Legacy human durations (e.g. 1h 30m) not parsed yet
            }
        }
        return null;
    }

    private CreateIssueRequest buildCreateIssueRequest(Map<String, Object> data, String projectId) {
        CreateIssueRequest.CreateIssueRequestBuilder builder = CreateIssueRequest.builder()
                .projectId(projectId)
                .issueType((String) data.getOrDefault("issueType", data.get("issue_type")))
                .summary((String) data.getOrDefault("summary", data.get("title")))
                .description((String) data.getOrDefault("description", data.get("body")))
                .status((String) data.getOrDefault("status", data.get("issue_status")))
                .priority((String) data.getOrDefault("priority", data.get("issue_priority")))
                .assigneeId((String) data.get("assigneeId"))
                .reporterId((String) data.get("reporterId"))
                .originalIssueKey((String) data.getOrDefault("originalIssueKey",
                        data.getOrDefault("issueKey", data.get("issue_key"))));

        Object createdAt = data.getOrDefault("createdAt", data.get("created"));
        if (createdAt != null) {
            java.time.LocalDateTime parsed = parseMigrationTimestamp(createdAt.toString());
            if (parsed != null) builder.migrationCreatedAt(parsed);
        }
        Object updatedAt = data.getOrDefault("updatedAt", data.get("updated"));
        if (updatedAt != null) {
            java.time.LocalDateTime parsed = parseMigrationTimestamp(updatedAt.toString());
            if (parsed != null) builder.migrationUpdatedAt(parsed);
        }

        // Handle labels (list or comma-separated Legacy export)
        Object labelsObj = data.get("labels");
        if (labelsObj instanceof List<?> list) {
            List<String> labels = new ArrayList<>();
            for (Object o : list) {
                if (o != null && !o.toString().isBlank()) {
                    labels.add(o.toString().trim());
                }
            }
            if (!labels.isEmpty()) {
                builder.labels(labels);
            }
        } else if (labelsObj instanceof String labelsStr && !labelsStr.isBlank()) {
            String trimmed = labelsStr.trim();
            if (!"0".equals(trimmed)) {
                builder.labels(Arrays.stream(trimmed.split("[,;]"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList());
            }
        }

        Long originalEstimate = parseDurationSeconds(data.get("original_estimate"), data.get("originalEstimate"));
        if (originalEstimate != null) {
            builder.originalEstimate(originalEstimate);
        }
        Long remainingEstimate = parseDurationSeconds(data.get("remaining_estimate"), data.get("remainingEstimate"));
        if (remainingEstimate != null) {
            builder.remainingEstimate(remainingEstimate);
        }
        Long timeSpent = parseDurationSeconds(data.get("time_spent"), data.get("timeSpent"));
        if (timeSpent != null) {
            builder.timeSpent(timeSpent);
        }

        // Handle parent
        String parentId = (String) data.getOrDefault("parentId", data.get("parent_id"));
        String parentIssueKey = (String) data.getOrDefault("parentIssueKey",
                data.getOrDefault("parent_key", data.get("parent")));
        if (parentId != null) {
            builder.parentId(parentId);
        }

        // Handle sprint
        String sprintId = (String) data.getOrDefault("sprintId", data.get("sprint_id"));
        if (sprintId != null) {
            builder.sprintId(sprintId);
        }

        // Handle epic link (also check epic_link)
        String epicId = (String) data.getOrDefault("epicId", data.get("epic_id"));
        if (epicId != null) {
            builder.epicId(epicId);
        }

        // Handle due date
        Object dueDate = data.getOrDefault("dueDate", data.get("due_date"));
        if (dueDate != null) {
            try {
                builder.dueDate(LocalDateTime.parse(dueDate.toString()));
            } catch (Exception e) {
                log.debug("Could not parse due date: {}", dueDate);
            }
        }

        // Handle story points
        Object storyPoints = data.getOrDefault("storyPoints", data.get("story_points"));
        if (storyPoints != null) {
            if (storyPoints instanceof Number) {
                builder.storyPoints(((Number) storyPoints).doubleValue());
            }
        }

        // Handle custom fields
        Object customFields = data.getOrDefault("customFields", data.get("custom_fields"));
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
        String expectedParentType = parentTypeMap.get(issueType);

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
        if (issueKey == null || issueKey.isBlank()) {
            return null;
        }
        try {
            return issueServiceClient.getIssueByKey(issueKey)
                    .map(IssueResponse::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Could not resolve issue ID for key {}: {}", issueKey, e.getMessage());
            return null;
        }
    }

    private static Integer extractRowNumber(Map<String, Object> issueData) {
        Object row = issueData.get("rowNumber");
        if (row instanceof Number n) {
            return n.intValue();
        }
        if (row != null) {
            try {
                return Integer.parseInt(row.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
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
            status.setStatus(success ? "COMPLETED" : "FAILED");
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
                failure.setRowNumber(extractRowNumber(issue));
                Object src = issue.getOrDefault("issueKey", issue.get("issue_key"));
                if (src != null) {
                    failure.setSourceIssueKey(src.toString());
                }
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
        private String sourceIssueKey;
        private String errorCode;
        private String errorMessage;
        private Integer rowNumber;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getIssueId() { return issueId; }
        public void setIssueId(UUID issueId) { this.issueId = issueId; }
        public String getIssueKey() { return issueKey; }
        public void setIssueKey(String issueKey) { this.issueKey = issueKey; }
        public String getSourceIssueKey() { return sourceIssueKey; }
        public void setSourceIssueKey(String sourceIssueKey) { this.sourceIssueKey = sourceIssueKey; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Integer getRowNumber() { return rowNumber; }
        public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
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

    private static final java.time.format.DateTimeFormatter[] MIGRATION_DATE_FORMATS = {
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        java.time.format.DateTimeFormatter.ofPattern("dd/MMM/yy h:mm a", java.util.Locale.ENGLISH),
        java.time.format.DateTimeFormatter.ofPattern("dd/MMM/yy hh:mm a", java.util.Locale.ENGLISH),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy h:mm a", java.util.Locale.ENGLISH),
        java.time.format.DateTimeFormatter.ofPattern("dd/MMM/yyyy h:mm a", java.util.Locale.ENGLISH),
    };

    private static java.time.LocalDateTime parseMigrationTimestamp(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.replace("Z", "").trim();
        for (var fmt : MIGRATION_DATE_FORMATS) {
            try {
                return java.time.LocalDateTime.parse(clean, fmt);
            } catch (Exception ignored) {}
        }
        log.debug("Could not parse migration timestamp: {}", value);
        return null;
    }
}