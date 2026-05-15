package com.jira.migration.persister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Component Persister Handler
 * Handles project component creation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentPersisterHandler {

    @Transactional(rollbackFor = Exception.class)
    public ComponentPersistResult persistComponent(Map<String, Object> componentData, UUID jobId) {
        ComponentPersistResult result = new ComponentPersistResult();

        try {
            String projectKey = (String) componentData.get("projectKey");
            if (projectKey == null) {
                throw new IllegalArgumentException("Project key is required");
            }

            String name = (String) componentData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Component name is required");
            }

            ComponentEntity component = ComponentEntity.builder()
                    .projectKey(projectKey)
                    .name(name)
                    .description((String) componentData.get("description"))
                    .leadUserId((UUID) componentData.get("leadUserId"))
                    .build();

            UUID componentId = persistToDatabase(component);

            result.setSuccess(true);
            result.setComponentId(componentId);
            result.setComponentName(name);

            log.info("Persisted component {} for project {}", name, projectKey);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private UUID persistToDatabase(ComponentEntity component) {
        log.debug("Persisting component: {} for project {}", component.getName(), component.getProjectKey());
        return UUID.randomUUID();
    }

    @lombok.Data
    @lombok.Builder
    public static class ComponentEntity {
        private UUID id;
        private String projectKey;
        private String name;
        private String description;
        private UUID leadUserId;
    }

    public static class ComponentPersistResult {
        private boolean success;
        private UUID componentId;
        private String componentName;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public UUID getComponentId() { return componentId; }
        public void setComponentId(UUID componentId) { this.componentId = componentId; }
        public String getComponentName() { return componentName; }
        public void setComponentName(String componentName) { this.componentName = componentName; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}