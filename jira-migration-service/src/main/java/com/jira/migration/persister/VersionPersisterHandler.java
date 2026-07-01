package com.jira.migration.persister;

import com.jira.migration.entity.ProjectMapping;
import com.jira.migration.repository.ProjectMappingRepository;
import com.jira.migration.service.clients.IssueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Version Persister Handler — creates versions via issue-service API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VersionPersisterHandler {

    private final IssueServiceClient issueServiceClient;
    private final ProjectMappingRepository projectMappingRepository;

    @Transactional(rollbackFor = Exception.class)
    public VersionPersistResult persistVersion(Map<String, Object> versionData, UUID jobId) {
        VersionPersistResult result = new VersionPersistResult();

        try {
            String projectKey = (String) versionData.get("projectKey");
            String projectId = (String) versionData.get("projectId");
            if (projectId == null && projectKey != null) {
                projectId = projectMappingRepository.findByJobIdAndSourceKey(jobId, projectKey)
                        .map(ProjectMapping::getTargetId)
                        .map(UUID::toString)
                        .orElse(null);
            }
            if (projectId == null) {
                throw new IllegalArgumentException("Project key or ID is required");
            }

            String name = (String) versionData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Version name is required");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("projectId", projectId);
            payload.put("name", name);
            if (versionData.get("description") != null) {
                payload.put("description", versionData.get("description"));
            }
            if (versionData.get("released") != null) {
                payload.put("isReleased", versionData.get("released"));
            }
            if (versionData.get("archived") != null) {
                payload.put("isArchived", versionData.get("archived"));
            }
            if (versionData.get("releaseDate") != null) {
                payload.put("releaseDate", versionData.get("releaseDate").toString());
            }

            Map<String, Object> response = issueServiceClient.createVersion(payload);
            Object id = response.get("id");
            UUID versionId = id != null ? UUID.fromString(id.toString()) : UUID.randomUUID();

            result.setSuccess(true);
            result.setVersionId(versionId);
            result.setVersionName(name);
            log.info("Persisted version {} for project {}", name, projectKey);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    public static class VersionPersistResult {
        private boolean success;
        private UUID versionId;
        private String versionName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getVersionId() { return versionId; }
        public void setVersionId(UUID versionId) { this.versionId = versionId; }
        public String getVersionName() { return versionName; }
        public void setVersionName(String versionName) { this.versionName = versionName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
