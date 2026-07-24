package com.jira.workflow.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Resolves project permissions via jira-project-service (Jira DC permission scheme).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectPermissionClient {

    private final RestTemplate restTemplate;

    @Value("${jira.services.project-url:http://localhost:8083}")
    private String projectServiceUrl;

    @Value("${jira.permissions.fail-open:false}")
    private boolean failOpen;

    public boolean isFailOpen() {
        return failOpen;
    }

    public boolean hasPermission(UUID userId, UUID projectId, String permission) {
        if (userId == null || projectId == null || permission == null || permission.isBlank()) {
            return failOpen;
        }
        try {
            String url = String.format("%s/api/projects/%s/permissions/check?userId=%s&permission=%s",
                    projectServiceUrl, projectId, userId, permission);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return Boolean.TRUE.equals(response != null ? response.get("hasPermission") : null);
        } catch (Exception e) {
            log.warn("Permission check failed user={} project={} perm={}: {}",
                    userId, projectId, permission, e.getMessage());
            return failOpen;
        }
    }
}
