package com.jira.admin.service;

import com.jira.admin.entity.*;
import com.jira.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise Permission Resolution Service.
 * Implements the full User→Group→Role→Permission chain for Jira DC compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionResolutionService {

    private final PermissionSchemeRepository permissionSchemeRepository;
    private final PermissionSchemeGrantRepository permissionSchemeGrantRepository;
    private final ProjectRoleActorRepository projectRoleActorRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;
    private final ProjectRepository projectRepository;

    // Permission holder types
    public static final String HOLDER_USER = "USER";
    public static final String HOLDER_GROUP = "GROUP";
    public static final String HOLDER_PROJECT_ROLE = "PROJECT_ROLE";

    @Value("${app.permissions.admin-group-id:grp-jira-administrators}")
    private String adminGroupId;

    @Value("${app.permissions.admin-role-id:role-admin}")
    private String adminRoleId;

    @Value("${app.permissions.group-id-prefix:grp-}")
    private String groupIdPrefix;

    /**
     * Check if user has a specific permission in a project.
     * Resolution chain: User → Groups → Roles → Permissions
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String projectId, String userId, String permissionKey) {
        // Get project's permission scheme
        Optional<PermissionSchemeEntity> scheme = getProjectPermissionScheme(projectId);
        if (scheme.isEmpty()) {
            log.debug("No permission scheme found for project {}", projectId);
            return false;
        }

        String schemeId = scheme.get().getId();

        // Collect all holder IDs (user direct + user's groups)
        List<String> holderIds = collectHolderIds(userId);

        // Check direct user permission
        if (hasDirectPermission(schemeId, userId, permissionKey)) {
            return true;
        }

        // Check group-based permissions
        if (hasGroupPermission(schemeId, holderIds.stream().filter(id -> isGroup(id)).collect(Collectors.toList()), permissionKey)) {
            return true;
        }

        // Check role-based permissions through project role actors
        if (hasRoleBasedPermission(projectId, userId, holderIds, permissionKey)) {
            return true;
        }

        return false;
    }

    /**
     * Get all effective permissions for a user in a project.
     */
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(String projectId, String userId) {
        Set<String> permissions = new HashSet<>();

        // Get project's permission scheme
        Optional<PermissionSchemeEntity> scheme = getProjectPermissionScheme(projectId);
        if (scheme.isEmpty()) {
            return permissions;
        }

        String schemeId = scheme.get().getId();
        List<String> holderIds = collectHolderIds(userId);

        // Get direct user permissions
        List<PermissionSchemeGrantEntity> userGrants = permissionSchemeGrantRepository
                .findBySchemeAndHolder(schemeId, HOLDER_USER, userId);
        for (PermissionSchemeGrantEntity grant : userGrants) {
            permissions.add(getPermissionKey(grant.getPermissionId()));
        }

        // Get group-based permissions
        List<String> userGroups = holderIds.stream().filter(this::isGroup).collect(Collectors.toList());
        if (!userGroups.isEmpty()) {
            List<PermissionSchemeGrantEntity> groupGrants = permissionSchemeGrantRepository
                    .findBySchemeAndHolders(schemeId, userGroups);
            for (PermissionSchemeGrantEntity grant : groupGrants) {
                permissions.add(getPermissionKey(grant.getPermissionId()));
            }
        }

        // Get role-based permissions
        Set<String> rolePermissions = getRoleBasedPermissions(projectId, userId, holderIds);
        permissions.addAll(rolePermissions);

        return permissions;
    }

    /**
     * Check if user can perform an action based on global (system) permission.
     */
    @Transactional(readOnly = true)
    public boolean hasGlobalPermission(String userId, String permissionKey) {
        // Check if user has global permission through any group membership
        List<String> userGroups = userGroupMembershipRepository.findGroupIdsByUserId(userId);

        // Global permissions are typically granted to specific groups
        // For now, check if user is in jira-administrators group
        if (userGroups.contains(adminGroupId)) {
            // Admin group has all global permissions
            return true;
        }

        return false;
    }

    /**
     * Check if user has any role in a project.
     */
    @Transactional(readOnly = true)
    public List<String> getUserRolesInProject(String projectId, String userId) {
        List<String> roles = new ArrayList<>();

        // Direct role assignment
        List<ProjectRoleActorEntity> directRoles = projectRoleActorRepository
                .findByProjectIdAndDirectUser(projectId, userId);
        for (ProjectRoleActorEntity actor : directRoles) {
            roles.add(actor.getProjectRoleId());
        }

        // Group-based role assignment
        List<String> userGroups = userGroupMembershipRepository.findGroupIdsByUserId(userId);
        if (!userGroups.isEmpty()) {
            List<ProjectRoleActorEntity> groupRoles = projectRoleActorRepository
                    .findByProjectIdAndGroups(projectId, userGroups);
            for (ProjectRoleActorEntity actor : groupRoles) {
                if (!roles.contains(actor.getProjectRoleId())) {
                    roles.add(actor.getProjectRoleId());
                }
            }
        }

        return roles;
    }

    /**
     * Check if user is a project administrator.
     */
    @Transactional(readOnly = true)
    public boolean isProjectAdmin(String projectId, String userId) {
        List<String> roles = getUserRolesInProject(projectId, userId);
        return roles.contains(adminRoleId);
    }

    /**
     * Check if user can browse a project.
     */
    @Transactional(readOnly = true)
    public boolean canBrowseProject(String projectId, String userId) {
        return hasPermission(projectId, userId, "BROWSE_PROJECTS");
    }

    /**
     * Check if user can edit issues in a project.
     */
    @Transactional(readOnly = true)
    public boolean canEditIssues(String projectId, String userId) {
        return hasPermission(projectId, userId, "EDIT_ISSUES");
    }

    /**
     * Check if user can create issues in a project.
     */
    @Transactional(readOnly = true)
    public boolean canCreateIssues(String projectId, String userId) {
        return hasPermission(projectId, userId, "CREATE_ISSUES");
    }

    /**
     * Check if user can delete issues in a project.
     */
    @Transactional(readOnly = true)
    public boolean canDeleteIssues(String projectId, String userId) {
        return hasPermission(projectId, userId, "DELETE_ISSUES");
    }

    /**
     * Check if user can manage sprints in a project.
     */
    @Transactional(readOnly = true)
    public boolean canManageSprints(String projectId, String userId) {
        return hasPermission(projectId, userId, "MANAGE_SPRINTS");
    }

    /**
     * Check if user can start/stop sprints in a project.
     */
    @Transactional(readOnly = true)
    public boolean canStartStopSprints(String projectId, String userId) {
        return hasPermission(projectId, userId, "START_SPRINT") || hasPermission(projectId, userId, "CLOSE_SPRINT");
    }

    /**
     * Get all users who have a specific permission in a project.
     */
    @Transactional(readOnly = true)
    public List<String> getUsersWithPermission(String projectId, String permissionKey) {
        Set<String> userIds = new HashSet<>();

        Optional<PermissionSchemeEntity> scheme = getProjectPermissionScheme(projectId);
        if (scheme.isEmpty()) {
            return new ArrayList<>();
        }

        String schemeId = scheme.get().getId();

        // Get direct user permissions
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository.findByPermissionSchemeId(schemeId);
        for (PermissionSchemeGrantEntity grant : grants) {
            if (grant.getHolderType().equals(HOLDER_USER) && hasPermissionKey(grant.getPermissionId(), permissionKey)) {
                userIds.add(grant.getHolderId());
            }
        }

        return new ArrayList<>(userIds);
    }

    // ===== Private helper methods =====

    private Optional<PermissionSchemeEntity> getProjectPermissionScheme(String projectId) {
        // Get project to find its permission scheme
        Optional<ProjectEntity> project = projectRepository.findById(projectId);
        if (project.isEmpty()) {
            return permissionSchemeRepository.findByIsDefaultTrue();
        }

        String schemeId = project.get().getPermissionSchemeId();
        if (schemeId == null) {
            return permissionSchemeRepository.findByIsDefaultTrue();
        }

        return permissionSchemeRepository.findById(schemeId);
    }

    private List<String> collectHolderIds(String userId) {
        List<String> holderIds = new ArrayList<>();
        holderIds.add(userId);  // Direct user

        // Add user's groups
        List<String> groups = userGroupMembershipRepository.findGroupIdsByUserId(userId);
        holderIds.addAll(groups);

        return holderIds;
    }

    private boolean hasDirectPermission(String schemeId, String userId, String permissionKey) {
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository
                .findBySchemeAndHolder(schemeId, HOLDER_USER, userId);
        return grants.stream().anyMatch(g -> hasPermissionKey(g.getPermissionId(), permissionKey));
    }

    private boolean hasGroupPermission(String schemeId, List<String> groupIds, String permissionKey) {
        if (groupIds.isEmpty()) {
            return false;
        }
        List<PermissionSchemeGrantEntity> grants = permissionSchemeGrantRepository
                .findBySchemeAndHolders(schemeId, groupIds);
        return grants.stream().anyMatch(g -> hasPermissionKey(g.getPermissionId(), permissionKey));
    }

    private boolean hasRoleBasedPermission(String projectId, String userId, List<String> holderIds, String permissionKey) {
        // Get user's roles in the project
        List<String> roles = getUserRolesInProject(projectId, userId);
        if (roles.isEmpty()) {
            return false;
        }

        // Check if any of user's roles have the required permission
        for (String roleId : roles) {
            List<String> rolePermissions = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
            for (String permId : rolePermissions) {
                if (hasPermissionKey(permId, permissionKey)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Set<String> getRoleBasedPermissions(String projectId, String userId, List<String> holderIds) {
        Set<String> permissions = new HashSet<>();
        List<String> roles = getUserRolesInProject(projectId, userId);

        for (String roleId : roles) {
            List<String> rolePermissions = rolePermissionRepository.findPermissionIdsByRoleId(roleId);
            for (String permId : rolePermissions) {
                String key = getPermissionKey(permId);
                if (key != null) {
                    permissions.add(key);
                }
            }
        }

        return permissions;
    }

    private boolean hasPermissionKey(String permissionId, String permissionKey) {
        if (permissionId == null || permissionKey == null) {
            return false;
        }
        // The permissionId field on grant entities stores the permission key directly
        // (e.g., "BROWSE_PROJECTS", "EDIT_ISSUES") so compare directly
        return permissionKey.equals(permissionId);
    }

    private String getPermissionKey(String permissionId) {
        // The permissionId field on grant entities stores the permission key directly
        return permissionId;
    }

    private boolean isGroup(String holderId) {
        return holderId != null && holderId.startsWith(groupIdPrefix);
    }
}