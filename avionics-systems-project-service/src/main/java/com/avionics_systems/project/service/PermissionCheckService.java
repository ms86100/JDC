package com.avionics_systems.project.service;

import com.avionics_systems.project.entity.Permission;
import com.avionics_systems.project.entity.PermissionScheme;
import com.avionics_systems.project.entity.Project;
import com.avionics_systems.project.repository.PermissionSchemeRepository;
import com.avionics_systems.project.repository.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Permission Checking Service - Legacy DC Compatible
 *
 * Evaluates whether a user has specific permissions within a project or globally.
 * This implements Avionics Systems permission checking logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckService {

    private final ProjectRepository projectRepository;
    private final PermissionSchemeRepository permissionSchemeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.defaults.system-admin-role:ROLE_ADMIN}")
    private String systemAdminRole;

    @Value("${app.defaults.administer-projects-permission:ADMINISTER_PROJECTS}")
    private String administerProjectsPermission;

    /**
     * Check if user has a specific permission in a project
     *
     * @param userId User UUID
     * @param projectId Project UUID
     * @param permissionKey Permission key (e.g., "EDIT_ISSUES", "ADMINISTER_PROJECTS")
     * @return true if user has the permission
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(UUID userId, UUID projectId, String permissionKey) {
        log.debug("Checking permission {} for user {} in project {}",
                  permissionKey, userId, projectId);

        // System admins have all permissions
        if (isSystemAdmin(userId)) {
            return true;
        }

        // Project admins have all project permissions
        if (permissionKey.equals(administerProjectsPermission) || hasPermission(userId, projectId, administerProjectsPermission)) {
            return true;
        }

        // Check via native SQL function (most efficient)
        return checkPermissionViaFunction(userId, projectId, permissionKey);
    }

    /**
     * Check if user has global admin permission
     * Queries jira_auth database for ROLE_ADMIN
     */
    @Transactional(readOnly = true)
    public boolean isSystemAdmin(UUID userId) {
        try {
            Boolean result = (Boolean) entityManager.createNativeQuery(
                "SELECT EXISTS(SELECT 1 FROM jira_auth.users u " +
                "JOIN jira_auth.user_roles ur ON u.id = ur.user_id " +
                "JOIN jira_auth.roles r ON ur.role_id = r.id " +
                "WHERE u.id = :userId AND r.name = :roleName)"
            ).setParameter("userId", userId)
             .setParameter("roleName", systemAdminRole)
             .getSingleResult();
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("Failed to check system admin status for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has any of the given permissions
     */
    @Transactional(readOnly = true)
    public boolean hasAnyPermission(UUID userId, UUID projectId, String... permissionKeys) {
        for (String key : permissionKeys) {
            if (hasPermission(userId, projectId, key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the given permissions
     */
    @Transactional(readOnly = true)
    public boolean hasAllPermissions(UUID userId, UUID projectId, String... permissionKeys) {
        for (String key : permissionKeys) {
            if (!hasPermission(userId, projectId, key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get default permission scheme for a project
     * Returns null if project doesn't have a custom scheme assigned
     */
    @Transactional(readOnly = true)
    public PermissionScheme getEffectivePermissionScheme(UUID projectId) {
        return projectRepository.findById(projectId)
            .map(Project::getPermissionScheme)
            .orElse(null);
    }

    /**
     * Check permission via native PostgreSQL function
     * Calls jira_project.check_permission() function for efficient permission evaluation
     */
    @Transactional(readOnly = true)
    private boolean checkPermissionViaFunction(UUID userId, UUID projectId, String permissionKey) {
        try {
            // Verify project exists first
            if (!projectRepository.existsById(projectId)) {
                log.warn("Project {} does not exist", projectId);
                return false;
            }

            // Call the PostgreSQL function
            Boolean result = (Boolean) entityManager.createNativeQuery(
                "SELECT jira_project.check_permission(:userId, :projectId, :permissionKey)"
            )
            .setParameter("userId", userId)
            .setParameter("projectId", projectId)
            .setParameter("permissionKey", permissionKey)
            .getSingleResult();

            log.debug("Permission check result: user={}, project={}, perm={}, result={}",
                      userId, projectId, permissionKey, result);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking permission via function: user={}, project={}, perm={}, error={}",
                      userId, projectId, permissionKey, e.getMessage());
            return false;
        }
    }

    // ==================== Common Permission Checks ====================

    /**
     * Can the user browse (view) this project?
     */
    public boolean canBrowseProject(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.BROWSE_PROJECTS);
    }

    /**
     * Can the user create issues in this project?
     */
    public boolean canCreateIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.CREATE_ISSUES);
    }

    /**
     * Can the user edit issues in this project?
     */
    public boolean canEditIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.EDIT_ISSUES);
    }

    /**
     * Can the user delete issues in this project?
     */
    public boolean canDeleteIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.DELETE_ISSUES);
    }

    /**
     * Can the user assign issues in this project?
     */
    public boolean canAssignIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.ASSIGN_ISSUES);
    }

    /**
     * Can the user resolve/close issues in this project?
     */
    public boolean canResolveIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.RESOLVE_ISSUES);
    }

    /**
     * Can the user administer this project?
     */
    public boolean canAdministerProject(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, administerProjectsPermission);
    }

    /**
     * Can the user comment on issues in this project?
     */
    public boolean canComment(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.CREATE_COMMENTS);
    }

    /**
     * Can the user attach files in this project?
     */
    public boolean canAttach(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.CREATE_ATTACHMENTS);
    }

    /**
     * Can the user work on issues (log work) in this project?
     */
    public boolean canWorkOnIssues(UUID userId, UUID projectId) {
        return hasPermission(userId, projectId, Permission.WORK_ON_ISSUES);
    }
}
