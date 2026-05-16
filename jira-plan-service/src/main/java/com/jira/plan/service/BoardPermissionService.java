package com.jira.plan.service;

import com.jira.plan.dto.request.CreateBoardPermissionRequest;
import com.jira.plan.dto.request.CreateProjectSprintPermissionRequest;
import com.jira.plan.dto.response.BoardPermissionResponse;
import com.jira.plan.dto.response.ProjectSprintPermissionResponse;
import com.jira.plan.entity.BoardConfig;
import com.jira.plan.entity.BoardPermission;
import com.jira.plan.entity.ProjectSprintPermission;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Board and project-level permission service with enterprise RBAC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardPermissionService {

    private final BoardPermissionRepository boardPermissionRepository;
    private final ProjectSprintPermissionRepository projectSprintPermissionRepository;
    private final BoardConfigRepository boardConfigRepository;
    private final UserGroupMembershipRepository userGroupMembershipRepository;

    // Permission types
    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_ADMIN = "ADMIN";
    public static final String PERMISSION_MANAGE_SPRINTS = "MANAGE_SPRINTS";
    public static final String PERMISSION_EDIT_SPRINTS = "EDIT_SPRINTS";

    // Sprint permission keys (mimics Jira GreenHopper)
    public static final String SPRINT_PERM_MANAGE = "MANAGE_SPRINTS";
    public static final String SPRINT_PERM_START_STOP = "START_STOP_SPRINTS";
    public static final String SPRINT_PERM_EDIT_NAME_GOAL = "EDIT_SPRINT_NAME_AND_GOAL";

    @Transactional(readOnly = true)
    public List<BoardPermissionResponse> getBoardPermissions(UUID boardId) {
        return boardPermissionRepository.findByBoardConfigId(boardId).stream()
            .map(this::toBoardPermissionResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public BoardPermissionResponse grantBoardPermission(UUID boardId, CreateBoardPermissionRequest request, UUID grantedBy) {
        BoardConfig board = boardConfigRepository.findById(boardId)
            .orElseThrow(() -> new ResourceNotFoundException("BoardConfig", "id", boardId));

        // Check if permission already exists
        boolean exists = boardPermissionRepository.existsByBoardConfigIdAndPermissionTypeAndPrincipalId(
            boardId, request.getPermissionType(), request.getPrincipalId());

        if (exists) {
            throw new IllegalArgumentException("Permission already exists");
        }

        BoardPermission permission = BoardPermission.builder()
            .boardConfig(board)
            .permissionType(request.getPermissionType())
            .principalType(request.getPrincipalType())
            .principalId(request.getPrincipalId())
            .grantedBy(grantedBy)
            .build();

        permission = boardPermissionRepository.save(permission);

        log.info("Granted {} permission on board {} to {}:{}",
            request.getPermissionType(), boardId, request.getPrincipalType(), request.getPrincipalId());

        return toBoardPermissionResponse(permission);
    }

    @Transactional
    public void revokeBoardPermission(UUID permissionId) {
        boardPermissionRepository.deleteById(permissionId);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID boardId, String permissionType, UUID userId) {
        // Check if user is board admin (admins have all permissions)
        if (boardPermissionRepository.existsByBoardConfigIdAndPermissionTypeAndPrincipalId(boardId, PERMISSION_ADMIN, userId)) {
            return true;
        }

        // Check for specific permission granted directly to user
        if (boardPermissionRepository.existsByBoardConfigIdAndPermissionTypeAndPrincipalId(boardId, permissionType, userId)) {
            return true;
        }

        // Check group memberships and group-based permissions
        List<UUID> userGroups = userGroupMembershipRepository.findGroupIdsByUserId(userId.toString());
        for (UUID groupId : userGroups) {
            if (boardPermissionRepository.existsByBoardConfigIdAndPermissionTypeAndPrincipalId(boardId, permissionType, groupId)) {
                return true;
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    public List<String> getEffectivePermissions(UUID boardId, UUID userId) {
        List<BoardPermission> directPermissions = boardPermissionRepository.findByBoardConfigIdAndPrincipalTypeAndPrincipalId(boardId, "USER", userId);

        // Also get group-based permissions
        List<UUID> userGroups = userGroupMembershipRepository.findGroupIdsByUserId(userId.toString());
        for (UUID groupId : userGroups) {
            List<BoardPermission> groupPerms = boardPermissionRepository.findByBoardConfigIdAndPrincipalTypeAndPrincipalId(boardId, "GROUP", groupId);
            directPermissions.addAll(groupPerms);
        }

        return directPermissions.stream()
            .map(BoardPermission::getPermissionType)
            .distinct()
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasBoardAccess(UUID boardId, UUID userId) {
        // Admins always have access
        if (hasPermission(boardId, PERMISSION_ADMIN, userId)) {
            return true;
        }

        // Check VIEW or any other permission
        return hasPermission(boardId, PERMISSION_VIEW, userId) ||
               hasPermission(boardId, PERMISSION_EDIT, userId);
    }

    // Project Sprint Permissions

    @Transactional(readOnly = true)
    public List<ProjectSprintPermissionResponse> getProjectSprintPermissions(UUID projectId) {
        return projectSprintPermissionRepository.findByProjectId(projectId).stream()
            .map(this::toProjectSprintPermissionResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProjectSprintPermissionResponse grantProjectSprintPermission(UUID projectId, CreateProjectSprintPermissionRequest request, UUID grantedBy) {
        // Check if permission already exists
        boolean exists = projectSprintPermissionRepository.existsByProjectIdAndPermissionKeyAndPrincipalId(
            projectId, request.getPermissionKey(), request.getPrincipalId());

        if (exists) {
            throw new IllegalArgumentException("Permission already exists");
        }

        ProjectSprintPermission permission = ProjectSprintPermission.builder()
            .projectId(projectId)
            .permissionKey(request.getPermissionKey())
            .principalType(request.getPrincipalType())
            .principalId(request.getPrincipalId())
            .createdBy(grantedBy)
            .build();

        permission = projectSprintPermissionRepository.save(permission);

        log.info("Granted sprint permission {} on project {} to {}:{}",
            request.getPermissionKey(), projectId, request.getPrincipalType(), request.getPrincipalId());

        return toProjectSprintPermissionResponse(permission);
    }

    @Transactional
    public void revokeProjectSprintPermission(UUID permissionId) {
        projectSprintPermissionRepository.deleteById(permissionId);
    }

    @Transactional(readOnly = true)
    public boolean hasProjectSprintPermission(UUID projectId, String permissionKey, UUID userId) {
        return projectSprintPermissionRepository.existsByProjectIdAndPermissionKeyAndPrincipalId(
            projectId, permissionKey, userId);
    }

    @Transactional(readOnly = true)
    public boolean canManageSprints(UUID projectId, UUID userId) {
        return hasProjectSprintPermission(projectId, SPRINT_PERM_MANAGE, userId);
    }

    @Transactional(readOnly = true)
    public boolean canStartStopSprints(UUID projectId, UUID userId) {
        return hasProjectSprintPermission(projectId, SPRINT_PERM_START_STOP, userId);
    }

    @Transactional(readOnly = true)
    public boolean canEditSprintNameAndGoal(UUID projectId, UUID userId) {
        return hasProjectSprintPermission(projectId, SPRINT_PERM_EDIT_NAME_GOAL, userId);
    }

    private BoardPermissionResponse toBoardPermissionResponse(BoardPermission permission) {
        return BoardPermissionResponse.builder()
            .id(permission.getId())
            .boardId(permission.getBoardConfig().getId())
            .permissionType(permission.getPermissionType())
            .principalType(permission.getPrincipalType())
            .principalId(permission.getPrincipalId())
            .grantedAt(permission.getGrantedAt())
            .grantedBy(permission.getGrantedBy())
            .build();
    }

    private ProjectSprintPermissionResponse toProjectSprintPermissionResponse(ProjectSprintPermission permission) {
        return ProjectSprintPermissionResponse.builder()
            .id(permission.getId())
            .projectId(permission.getProjectId())
            .permissionKey(permission.getPermissionKey())
            .principalType(permission.getPrincipalType())
            .principalId(permission.getPrincipalId())
            .createdAt(permission.getCreatedAt())
            .createdBy(permission.getCreatedBy())
            .build();
    }
}