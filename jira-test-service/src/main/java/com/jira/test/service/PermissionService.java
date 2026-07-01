package com.jira.test.service;

import com.jira.test.entity.UserProjectAccess;
import com.jira.test.repository.UserProjectAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserProjectAccessRepository accessRepository;

    public boolean hasAccess(UUID userId, UUID projectId) {
        return accessRepository.existsByUserIdAndProjectId(userId, projectId);
    }

    public boolean hasCreatePermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasCreatePermission)
                .orElse(false);
    }

    public boolean hasUpdatePermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasUpdatePermission)
                .orElse(false);
    }

    public boolean hasDeletePermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasDeletePermission)
                .orElse(false);
    }

    public boolean hasExecutePermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasExecutePermission)
                .orElse(false);
    }

    public boolean hasImportPermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasImportPermission)
                .orElse(false);
    }

    public boolean hasReportPermission(UUID userId, UUID projectId) {
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasReportPermission)
                .orElse(false);
    }

    public boolean isAdmin(UUID userId, UUID projectId) {
        return accessRepository.existsByUserIdAndProjectIdAndRole(userId, projectId, "ADMIN");
    }

    public boolean hasProjectRole(UUID userId, UUID projectId, String role) {
        return accessRepository.existsByUserIdAndProjectIdAndRole(userId, projectId, role);
    }
}