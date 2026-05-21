package com.jira.issue.service;

import com.jira.issue.security.PermissionCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class ProjectPermissionClient {

    @Value("${project.service.url}")
    private String projectServiceUrl;

    /**
     * When true (local dev only), permission checks succeed if project-service is down.
     * Production must set {@code jira.permissions.fail-open: false}.
     */
    @Value("${jira.permissions.fail-open:false}")
    private boolean failOpen;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isFailOpen() {
        return failOpen;
    }

    public PermissionCheckResult check(UUID userId, UUID projectId, String permission) {
        if (userId == null || projectId == null || permission == null || permission.isBlank()) {
            return failOpen ? PermissionCheckResult.GRANTED : PermissionCheckResult.DENIED;
        }
        try {
            String url = String.format("%s/api/projects/%s/permissions/check?userId=%s&permission=%s",
                    projectServiceUrl, projectId, userId, permission);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            boolean granted = Boolean.TRUE.equals(response != null ? response.get("hasPermission") : null);
            return granted ? PermissionCheckResult.GRANTED : PermissionCheckResult.DENIED;
        } catch (Exception e) {
            log.warn("Permission check failed user={} project={} perm={}: {}",
                    userId, projectId, permission, e.getMessage());
            return failOpen ? PermissionCheckResult.GRANTED : PermissionCheckResult.UNAVAILABLE;
        }
    }

    /** @deprecated use {@link #check(UUID, UUID, String)} */
    public boolean hasPermission(UUID userId, UUID projectId, String permission) {
        return check(userId, projectId, permission) == PermissionCheckResult.GRANTED;
    }
}
