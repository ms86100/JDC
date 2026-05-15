package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Notification Scheme Persister Handler
 * Handles notification scheme creation and notification rules
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationSchemePersisterHandler {

    @Transactional(rollbackFor = Exception.class)
    public NotificationSchemePersistResult persistNotificationScheme(
            Map<String, Object> schemeData,
            UUID jobId) {

        NotificationSchemePersistResult result = new NotificationSchemePersistResult();

        try {
            String name = (String) schemeData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Notification scheme name is required");
            }

            // 1. Create notification scheme
            NotificationSchemeEntity scheme = NotificationSchemeEntity.builder()
                    .name(name)
                    .description((String) schemeData.get("description"))
                    .isDefault((Boolean) schemeData.getOrDefault("isDefault", false))
                    .build();

            UUID schemeId = persistSchemeToDatabase(scheme);

            // 2. Persist notification events and recipients
            List<Map<String, Object>> notifications = (List<Map<String, Object>>) schemeData.get("notifications");
            if (notifications != null) {
                for (Map<String, Object> notification : notifications) {
                    persistNotification(schemeId, notification);
                }
            }

            result.setSuccess(true);
            result.setSchemeId(schemeId);
            result.setSchemeName(name);

            log.info("Persisted notification scheme: {}", name);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private UUID persistSchemeToDatabase(NotificationSchemeEntity scheme) {
        log.debug("Persisting notification scheme: {}", scheme.getName());
        return UUID.randomUUID();
    }

    private void persistNotification(UUID schemeId, Map<String, Object> notification) {
        String event = (String) notification.get("event"); // ISSUE_CREATED, ISSUE_UPDATED, etc.
        String recipientType = (String) notification.get("recipientType"); // USER, GROUP, PROJECT_ROLE, WATCHER
        UUID recipientId = (UUID) notification.get("recipientId");
        String groupName = (String) notification.get("groupName");

        log.debug("Persisting notification: event={}, type={}, recipient={}",
                event, recipientType, recipientId != null ? recipientId : groupName);

        // In production: Persist to notification_scheme_events table
    }

    /**
     * Assign notification scheme to project
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignSchemeToProject(UUID schemeId, UUID projectId) {
        log.info("Assigning notification scheme {} to project {}", schemeId, projectId);
        // In production: Persist to project_notification_scheme table
    }

    @lombok.Data
    @lombok.Builder
    public static class NotificationSchemeEntity {
        private UUID id;
        private String name;
        private String description;
        private Boolean isDefault;
    }

    public static class NotificationSchemePersistResult {
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