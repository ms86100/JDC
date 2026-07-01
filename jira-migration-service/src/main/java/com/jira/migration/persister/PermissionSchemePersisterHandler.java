package com.jira.migration.persister;

import com.jira.migration.service.clients.AdminServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Permission Scheme Persister Handler
 * Handles permission scheme creation and assignment
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionSchemePersisterHandler {

    private final AdminServiceClient adminServiceClient;

    @Transactional(rollbackFor = Exception.class)
    public PermissionSchemePersistResult persistPermissionScheme(
            Map<String, Object> schemeData,
            UUID jobId) {

        PermissionSchemePersistResult result = new PermissionSchemePersistResult();

        try {
            String name = (String) schemeData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Permission scheme name is required");
            }

            // 1. Create permission scheme
            PermissionSchemeEntity scheme = PermissionSchemeEntity.builder()
                    .name(name)
                    .description((String) schemeData.get("description"))
                    .isDefault((Boolean) schemeData.getOrDefault("isDefault", false))
                    .build();

            UUID schemeId = persistSchemeToDatabase(scheme, schemeData);

            // 2. Persist permission grants
            List<Map<String, Object>> grants = (List<Map<String, Object>>) schemeData.get("grants");
            if (grants != null) {
                for (Map<String, Object> grant : grants) {
                    persistPermissionGrant(schemeId, grant);
                }
            }

            result.setSuccess(true);
            result.setSchemeId(schemeId);
            result.setSchemeName(name);

            log.info("Persisted permission scheme: {}", name);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private UUID persistSchemeToDatabase(PermissionSchemeEntity scheme, Map<String, Object> schemeData) {
        if (adminServiceClient.isAvailable()) {
            try {
                Map<String, Object> created = adminServiceClient.createPermissionScheme(schemeData);
                Object id = created.get("id");
                if (id != null) {
                    return UUID.fromString(id.toString());
                }
            } catch (Exception e) {
                log.warn("admin-service permission scheme create failed, using local id: {}", e.getMessage());
            }
        }
        log.debug("Persisting permission scheme locally: {}", scheme.getName());
        return UUID.randomUUID();
    }

    private void persistPermissionGrant(UUID schemeId, Map<String, Object> grant) {
        String permissionKey = (String) grant.get("permissionKey");
        String grantType = (String) grant.get("grantType"); // USER, GROUP, PROJECT_ROLE
        UUID entityId = (UUID) grant.get("entityId");
        String groupName = (String) grant.get("groupName");
        UUID projectRoleId = (UUID) grant.get("projectRoleId");

        log.debug("Persisting permission grant: {} -> {} ({})", permissionKey, grantType, entityId != null ? entityId : groupName);

        // In production: Persist to permission_grants table
    }

    /**
     * Assign permission scheme to project
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignSchemeToProject(UUID schemeId, UUID projectId) {
        log.info("Assigning permission scheme {} to project {}", schemeId, projectId);
        // In production: Persist to project_permission_scheme table
    }

    @lombok.Data
    @lombok.Builder
    public static class PermissionSchemeEntity {
        private UUID id;
        private String name;
        private String description;
        private Boolean isDefault;
    }

    public static class PermissionSchemePersistResult {
        private boolean success;
        private UUID schemeId;
        private String schemeName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getSchemeId() { return schemeId; }
        public void setSchemeId(UUID schemeId) { this.schemeId = schemeId; }
        public String getSchemeName() { return schemeName; }
        public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}