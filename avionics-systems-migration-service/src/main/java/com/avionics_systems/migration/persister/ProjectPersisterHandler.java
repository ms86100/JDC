package com.avionics_systems.migration.persister;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.avionics_systems.migration.entity.EntityStatus;
import com.avionics_systems.migration.entity.ProjectMapping;
import com.avionics_systems.migration.exception.*;
import com.avionics_systems.migration.repository.*;
import com.avionics_systems.migration.security.MigrationRequestContext;
import com.avionics_systems.migration.service.clients.*;
import com.avionics_systems.migration.service.clients.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Project Persister Handler
 * Handles project entity creation with transactional integrity using real service calls.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectPersisterHandler {

    @Value("${app.project.default-type:COMPANY_MANAGED}")
    private String defaultProjectType;

    private final ProjectMappingRepository projectMappingRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final IssuePersisterHandler issuePersisterHandler;
    private final WorkflowPersisterHandler workflowPersisterHandler;
    private final CustomFieldPersisterHandler customFieldPersisterHandler;
    private final ProjectServiceClient projectServiceClient;
    private final MigrationJobRepository migrationJobRepository;
    private final ObjectMapper objectMapper;

    /**
     * Track created entities for rollback support.
     */
    private final List<String> createdProjectIds = new ArrayList<>();

    @Transactional(rollbackFor = Exception.class)
    public ProjectPersisterResult persistProject(Map<String, Object> projectData, UUID jobId) {
        ProjectPersisterResult result = new ProjectPersisterResult();

        try {
            if (MigrationRequestContext.getUserId() == null) {
                migrationJobRepository.findById(jobId)
                        .map(com.avionics_systems.migration.entity.MigrationJob::getInitiatedBy)
                        .ifPresent(MigrationRequestContext::setUserId);
            }
            // 1. Extract and validate project key
            String sourceKey = (String) projectData.get("projectKey");
            if (sourceKey == null || sourceKey.isBlank()) {
                throw new ValidationException("Project key is required", "PROJECT_KEY_REQUIRED", "projectKey");
            }

            // Validate key format (Legacy DC compatible)
            if (!sourceKey.matches("^[A-Z][A-Z0-9]{0,9}$")) {
                throw new ValidationException(
                        "Project key must be uppercase letters and numbers, starting with a letter (max 10 chars)",
                        "INVALID_PROJECT_KEY",
                        "projectKey"
                );
            }

            // 2. Check for duplicate key in target
            if (projectMappingRepository.existsByJobIdAndSourceKey(jobId, sourceKey)) {
                throw new MigrationException("Project key already exists: " + sourceKey, "DUPLICATE_PROJECT_KEY");
            }

            // 3. Generate target key (handle conflicts)
            String targetKey = generateUniqueTargetKey(sourceKey, jobId);

            // 4. Build project creation request
            CreateProjectRequest request = buildCreateProjectRequest(projectData, targetKey);

            // 5. Call real project service
            ProjectResponse response = createProjectWithRetry(request);

            // 6. Create project mapping
            UUID projectId = UUID.fromString(response.getId());
            ProjectMapping mapping = ProjectMapping.builder()
                    .jobId(jobId)
                    .sourceKey(sourceKey)
                    .targetKey(targetKey)
                    .targetId(projectId)
                    .issueKeySequence(0)
                    .build();
            projectMappingRepository.save(mapping);

            // Track for potential rollback
            createdProjectIds.add(response.getId());

            // 7. Update entity status
            updateEntityStatus(jobId, sourceKey, response.getId(), "PROJECT", true);

            result.setSuccess(true);
            result.setProjectId(projectId);
            result.setTargetKey(targetKey);

            log.info("Persisted project: {} -> {} with ID: {}", sourceKey, targetKey, response.getId());

        } catch (ValidationException e) {
            result.setSuccess(false);
            result.setErrorCode(e.getErrorCode());
            result.setErrorMessage(e.getMessage());
            throw e;
        } catch (ServiceClientException e) {
            result.setSuccess(false);
            result.setErrorCode("PROJECT_SERVICE_ERROR");
            result.setErrorMessage(e.getMessage());
            log.error("Project service error: {}", e.getMessage(), e);
            throw new MigrationException("Failed to create project in service: " + e.getMessage(), e);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorCode("PERSIST_ERROR");
            result.setErrorMessage(e.getMessage());
            log.error("Failed to persist project: {}", e.getMessage(), e);
            throw new MigrationException("Failed to persist project: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Create project with retry logic for transient failures.
     */
    private ProjectResponse createProjectWithRetry(CreateProjectRequest request) {
        int maxRetries = 3;
        long baseDelayMs = 1000;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return projectServiceClient.createProject(request);
            } catch (ServiceClientException e) {
                if (e.isRetryable() && attempt < maxRetries) {
                    log.warn("Project creation failed (attempt {}/{}), retrying in {}ms: {}",
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
        throw new MigrationException("Project creation failed after " + maxRetries + " attempts");
    }

    private String generateUniqueTargetKey(String sourceKey, UUID jobId) {
        String baseKey = sourceKey.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (baseKey.length() > 10) {
            baseKey = baseKey.substring(0, 10);
        }

        int suffix = 0;
        String candidateKey = baseKey;

        while (projectMappingRepository.existsByJobIdAndTargetKey(jobId, candidateKey)) {
            suffix++;
            if (suffix > 100) {
                throw new MigrationException("Could not generate unique project key for: " + sourceKey);
            }
            String suffixStr = String.valueOf(suffix);
            int maxBase = Math.max(2, 10 - suffixStr.length());
            candidateKey = baseKey.substring(0, Math.min(baseKey.length(), maxBase)) + suffixStr;
        }

        return candidateKey;
    }

    private CreateProjectRequest buildCreateProjectRequest(Map<String, Object> data, String targetKey) {
        return CreateProjectRequest.builder()
                .key(targetKey)
                .name((String) data.getOrDefault("name", targetKey))
                .description((String) data.get("description"))
                .leadUserId((String) data.get("leadUserId"))
                .projectType((String) data.getOrDefault("projectType", defaultProjectType))
                .projectTemplate((String) data.get("templateId"))
                .category((String) data.get("category"))
                .avatarUrl((String) data.get("avatarUrl"))
                .originalProjectKey((String) data.get("projectKey"))
                .build();
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
            status.setErrorMessage(null);

            entityStatusRepository.save(status);
        } catch (Exception e) {
            log.warn("Failed to update entity status for {}: {}", sourceKey, e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistProjectWithDependencies(
            Map<String, Object> projectData,
            List<Map<String, Object>> issueData,
            List<Map<String, Object>> workflowData,
            List<Map<String, Object>> customFieldData,
            UUID jobId) {

        log.info("Persisting project with {} issues, {} workflows, {} custom fields",
                issueData.size(), workflowData.size(), customFieldData.size());

        try {
            // 1. Persist project
            ProjectPersisterResult projectResult = persistProject(projectData, jobId);
            UUID projectId = projectResult.getProjectId();
            String projectIdStr = projectId.toString();

            // 2. Persist workflows first (issues depend on them)
            for (Map<String, Object> workflow : workflowData) {
                workflow.put("projectId", projectIdStr);
                workflowPersisterHandler.persistWorkflow(workflow, jobId);
            }

            // 3. Persist custom fields
            for (Map<String, Object> customField : customFieldData) {
                customField.put("projectId", projectIdStr);
                customFieldPersisterHandler.persistCustomField(customField, jobId);
            }

            // 4. Persist issues (with proper ordering for parent-child relationships)
            List<Map<String, Object>> sortedIssues = sortIssuesByHierarchy(issueData);

            for (Map<String, Object> issue : sortedIssues) {
                issue.put("projectId", projectIdStr);
                issuePersisterHandler.persistIssue(issue, jobId);
            }

            log.info("Successfully persisted project with all dependencies");

        } catch (Exception e) {
            log.error("Failed to persist project with dependencies", e);
            // Rollback created entities
            rollbackCreatedEntities();
            throw new MigrationException("Project import failed: " + e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> sortIssuesByHierarchy(List<Map<String, Object>> issues) {
        List<Map<String, Object>> sorted = new ArrayList<>();
        Set<String> processed = new HashSet<>();

        // First pass: add epics (issues without parent)
        for (Map<String, Object> issue : issues) {
            String issueType = (String) issue.getOrDefault("issueType", "");
            if ("Epic".equalsIgnoreCase(issueType)) {
                sorted.add(issue);
                processed.add((String) issue.get("issueKey"));
            }
        }

        // Second pass: add stories and tasks (with Epic parent)
        for (Map<String, Object> issue : issues) {
            String issueKey = (String) issue.get("issueKey");
            if (!processed.contains(issueKey)) {
                String parentKey = (String) issue.get("parentIssueKey");
                if (parentKey != null && processed.contains(parentKey)) {
                    sorted.add(issue);
                    processed.add(issueKey);
                }
            }
        }

        // Third pass: add subtasks (with Story/Task parent)
        for (Map<String, Object> issue : issues) {
            String issueKey = (String) issue.get("issueKey");
            if (!processed.contains(issueKey)) {
                String issueType = (String) issue.getOrDefault("issueType", "");
                if ("Subtask".equalsIgnoreCase(issueType)) {
                    String parentKey = (String) issue.get("parentIssueKey");
                    if (parentKey != null && processed.contains(parentKey)) {
                        sorted.add(issue);
                        processed.add(issueKey);
                    }
                }
            }
        }

        // Fourth pass: add any remaining issues
        for (Map<String, Object> issue : issues) {
            String issueKey = (String) issue.get("issueKey");
            if (!processed.contains(issueKey)) {
                sorted.add(issue);
                processed.add(issueKey);
            }
        }

        return sorted;
    }

    /**
     * Rollback created entities on failure.
     */
    public void rollbackCreatedEntities() {
        log.info("Rolling back {} created projects", createdProjectIds.size());
        for (String projectId : createdProjectIds) {
            try {
                projectServiceClient.deleteProject(projectId);
                log.debug("Rolled back project: {}", projectId);
            } catch (Exception e) {
                log.error("Failed to rollback project {}: {}", projectId, e.getMessage());
            }
        }
        createdProjectIds.clear();
    }

    /**
     * Clear rollback tracking (call after successful commit).
     */
    public void clearRollbackTracking() {
        createdProjectIds.clear();
    }

    public static class ProjectPersisterResult {
        private boolean success;
        private UUID projectId;
        private String targetKey;
        private String errorCode;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getTargetKey() { return targetKey; }
        public void setTargetKey(String targetKey) { this.targetKey = targetKey; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}