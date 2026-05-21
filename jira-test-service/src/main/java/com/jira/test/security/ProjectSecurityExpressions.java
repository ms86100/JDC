package com.jira.test.security;

import com.jira.test.entity.UserProjectAccess;
import com.jira.test.repository.UserProjectAccessRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component("projectSecurity")
@Slf4j
public class ProjectSecurityExpressions {

    private final UserProjectAccessRepository accessRepository;

    public ProjectSecurityExpressions(UserProjectAccessRepository accessRepository) {
        this.accessRepository = accessRepository;
    }

    public boolean canCreateTests(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasCreatePermission)
                .orElse(false);
    }

    public boolean canUpdateTests(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasUpdatePermission)
                .orElse(false);
    }

    public boolean canDeleteTests(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasDeletePermission)
                .orElse(false);
    }

    public boolean canExecuteTests(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasExecutePermission)
                .orElse(false);
    }

    public boolean canImportTests(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasImportPermission)
                .orElse(false);
    }

    public boolean canViewReports(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.findByUserIdAndProjectId(userId, projectId)
                .map(UserProjectAccess::getHasReportPermission)
                .orElse(false);
    }

    public boolean hasProjectAccess(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.existsByUserIdAndProjectId(userId, projectId);
    }

    public boolean isProjectAdmin(Authentication auth, UUID projectId) {
        UUID userId = getUserId(auth);
        return accessRepository.existsByUserIdAndProjectIdAndRole(userId, projectId, "ADMIN");
    }

    private UUID getUserId(Authentication auth) {
        try {
            return UUID.fromString(auth.getName());
        } catch (Exception e) {
            log.debug("Could not parse user ID from auth name: {}", auth.getName());
            return UUID.nameUUIDFromBytes(auth.getName().getBytes());
        }
    }
}