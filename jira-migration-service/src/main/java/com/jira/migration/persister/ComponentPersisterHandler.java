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
 * Component Persister Handler — creates components via issue-service API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComponentPersisterHandler {

    private final IssueServiceClient issueServiceClient;
    private final ProjectMappingRepository projectMappingRepository;

    @Transactional(rollbackFor = Exception.class)
    public ComponentPersistResult persistComponent(Map<String, Object> componentData, UUID jobId) {
        ComponentPersistResult result = new ComponentPersistResult();

        try {
            String projectKey = (String) componentData.get("projectKey");
            String projectId = (String) componentData.get("projectId");
            if (projectId == null && projectKey != null) {
                projectId = projectMappingRepository.findByJobIdAndSourceKey(jobId, projectKey)
                        .map(ProjectMapping::getTargetId)
                        .map(UUID::toString)
                        .orElse(null);
            }
            if (projectId == null) {
                throw new IllegalArgumentException("Project key or ID is required");
            }

            String name = (String) componentData.get("name");
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Component name is required");
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("projectId", projectId);
            payload.put("name", name);
            if (componentData.get("description") != null) {
                payload.put("description", componentData.get("description"));
            }

            Map<String, Object> response = issueServiceClient.createComponent(payload);
            Object id = response.get("id");
            UUID componentId = id != null ? UUID.fromString(id.toString()) : UUID.randomUUID();

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
