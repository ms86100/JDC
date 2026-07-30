package com.avionics_systems.issue.security;

import com.avionics_systems.issue.exception.PermissionDeniedException;
import com.avionics_systems.issue.exception.PermissionServiceUnavailableException;
import com.avionics_systems.issue.service.ProjectPermissionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Central RBAC enforcement for issue-service (fail-closed in production).
 */
@Component
@RequiredArgsConstructor
public class ProjectPermissionGuard {

    private final ProjectPermissionClient projectPermissionClient;

    public void requireUser(UUID userId) {
        if (userId == null) {
            throw new PermissionDeniedException("Authentication required (X-User-Id)");
        }
    }

    public void requirePermission(UUID userId, UUID projectId, String permission) {
        requireUser(userId);
        PermissionCheckResult result = projectPermissionClient.check(userId, projectId, permission);
        switch (result) {
            case GRANTED -> { /* ok */ }
            case DENIED -> throw new PermissionDeniedException(permission, "project " + projectId);
            case UNAVAILABLE -> throw new PermissionServiceUnavailableException(
                    "Permission service unavailable; cannot verify " + permission);
        }
    }

    public boolean hasPermission(UUID userId, UUID projectId, String permission) {
        if (userId == null || projectId == null) {
            return false;
        }
        return projectPermissionClient.check(userId, projectId, permission) == PermissionCheckResult.GRANTED;
    }
}
