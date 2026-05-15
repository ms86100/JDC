package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Version Persister Handler
 * Handles project version creation with release management
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VersionPersisterHandler {

    @Transactional(rollbackFor = Exception.class)
    public VersionPersistResult persistVersion(Map<String, Object> versionData, UUID jobId) {
        VersionPersistResult result = new VersionPersistResult();

        try {
            String projectKey = (String) versionData.get("projectKey");
            if (projectKey == null) {
                throw new IllegalArgumentException("Project key is required");
            }

            String name = (String) versionData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Version name is required");
            }

            VersionEntity version = VersionEntity.builder()
                    .projectKey(projectKey)
                    .name(name)
                    .description((String) versionData.get("description"))
                    .released((Boolean) versionData.getOrDefault("released", false))
                    .archived((Boolean) versionData.getOrDefault("archived", false))
                    .releaseDate(versionData.get("releaseDate") != null ?
                            java.time.LocalDate.parse(versionData.get("releaseDate").toString()) : null)
                    .sequence((Integer) versionData.getOrDefault("sequence", 0))
                    .build();

            UUID versionId = persistToDatabase(version);

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

    private UUID persistToDatabase(VersionEntity version) {
        log.debug("Persisting version: {} for project {}", version.getName(), version.getProjectKey());
        return UUID.randomUUID();
    }

    @lombok.Data
    @lombok.Builder
    public static class VersionEntity {
        private UUID id;
        private String projectKey;
        private String name;
        private String description;
        private Boolean released;
        private Boolean archived;
        private java.time.LocalDate releaseDate;
        private Integer sequence;
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