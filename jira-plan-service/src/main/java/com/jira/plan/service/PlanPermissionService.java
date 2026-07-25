package com.jira.plan.service;

import com.jira.plan.entity.Plan;
import com.jira.plan.entity.PlanPermission;
import com.jira.plan.exception.ResourceNotFoundException;
import com.jira.plan.exception.ForbiddenException;
import com.jira.plan.repository.PlanPermissionRepository;
import com.jira.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanPermissionService {

    private final PlanPermissionRepository permissionRepository;
    private final PlanRepository planRepository;

    @Value("${app.program.default-access-type:OPEN}")
    private String openAccessType;

    @Transactional
    public PlanPermission grantPermission(UUID planId, PlanPermission.PermissionType permissionType,
                                         PlanPermission.PrincipalType principalType,
                                         UUID principalId, UUID grantedBy) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        Optional<PlanPermission> existing = permissionRepository
                .findByPlanIdAndPermissionTypeAndPrincipalTypeAndPrincipalId(
                        planId, permissionType, principalType, principalId);

        if (existing.isPresent()) {
            return existing.get();
        }

        PlanPermission permission = PlanPermission.builder()
                .planId(planId)
                .permissionType(permissionType)
                .principalType(principalType)
                .principalId(principalId)
                .grantedBy(grantedBy)
                .build();

        return permissionRepository.save(permission);
    }

    @Transactional
    public void revokePermission(UUID planId, PlanPermission.PermissionType permissionType,
                                PlanPermission.PrincipalType principalType, UUID principalId) {
        PlanPermission permission = permissionRepository
                .findByPlanIdAndPermissionTypeAndPrincipalTypeAndPrincipalId(
                        planId, permissionType, principalType, principalId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", null));
        permissionRepository.delete(permission);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(UUID planId, PlanPermission.PermissionType permissionType,
                                UUID userId, List<UUID> groupIds) {
        if (planRepository.findById(planId).map(Plan::getIsActive).orElse(false) == false) {
            return false;
        }

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        if (isPublicPlan(plan)) {
            return permissionType == PlanPermission.PermissionType.VIEW ||
                   hasExplicitPermission(planId, permissionType, userId, groupIds);
        }

        return hasExplicitPermission(planId, permissionType, userId, groupIds);
    }

    private boolean isPublicPlan(Plan plan) {
        Object accessType = plan.getSettings().get("accessType");
        return accessType != null && accessType.toString().equalsIgnoreCase(openAccessType);
    }

    private boolean hasExplicitPermission(UUID planId, PlanPermission.PermissionType permissionType,
                                          UUID userId, List<UUID> groupIds) {
        if (userId != null) {
            if (permissionRepository.hasPermission(planId, permissionType,
                    PlanPermission.PrincipalType.USER, userId)) {
                return true;
            }
        }

        if (groupIds != null && !groupIds.isEmpty()) {
            List<PlanPermission> groupPermissions = permissionRepository.findByGroupIds(groupIds);
            for (PlanPermission permission : groupPermissions) {
                if (permission.getPlanId().equals(planId) &&
                    permission.getPermissionType() == permissionType) {
                    return true;
                }
            }
        }

        return false;
    }

    @Transactional(readOnly = true)
    public List<PlanPermission> getPlanPermissions(UUID planId) {
        return permissionRepository.findByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<PlanPermission> getUserPermissions(UUID planId, UUID userId, List<UUID> groupIds) {
        List<PlanPermission> permissions = new ArrayList<>();

        permissions.addAll(permissionRepository.findByPlanIdAndPrincipalId(planId, userId));

        if (groupIds != null && !groupIds.isEmpty()) {
            List<PlanPermission> groupPerms = permissionRepository.findByGroupIds(groupIds);
            permissions.addAll(groupPerms.stream()
                    .filter(p -> p.getPlanId().equals(planId))
                    .toList());
        }

        return permissions;
    }

    @Transactional(readOnly = true)
    public Set<PlanPermission.PermissionType> getUserPermissionSet(UUID planId, UUID userId, List<UUID> groupIds) {
        Set<PlanPermission.PermissionType> permissions = new HashSet<>();

        List<PlanPermission> userPermissions = getUserPermissions(planId, userId, groupIds);
        for (PlanPermission permission : userPermissions) {
            permissions.add(permission.getPermissionType());
        }

        return permissions;
    }

    public void checkPermission(UUID planId, PlanPermission.PermissionType requiredPermission,
                               UUID userId, List<UUID> groupIds) {
        if (!hasPermission(planId, requiredPermission, userId, groupIds)) {
            throw new ForbiddenException(
                    "User does not have " + requiredPermission + " permission on plan " + planId);
        }
    }

    @Transactional
    public void transferOwnership(UUID planId, UUID newOwnerId, UUID currentOwnerId) {
        grantPermission(planId, PlanPermission.PermissionType.ADMIN,
                PlanPermission.PrincipalType.USER, newOwnerId, currentOwnerId);

        permissionRepository.deleteByPlanIdAndPrincipalId(planId, currentOwnerId);

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
        plan.setOwnerId(newOwnerId);
        planRepository.save(plan);
    }

    @Transactional(readOnly = true)
    public List<UUID> getAccessiblePlanIds(UUID userId, List<UUID> groupIds,
                                          PlanPermission.PermissionType permissionType) {
        Set<UUID> accessiblePlanIds = new HashSet<>();

        List<PlanPermission> userPerms = permissionRepository.findByUserId(userId);
        for (PlanPermission perm : userPerms) {
            if (perm.getPermissionType() == permissionType ||
                permissionType == PlanPermission.PermissionType.VIEW) {
                accessiblePlanIds.add(perm.getPlanId());
            }
        }

        List<PlanPermission> groupPerms = permissionRepository.findByGroupIds(groupIds);
        for (PlanPermission perm : groupPerms) {
            if (perm.getPermissionType() == permissionType ||
                permissionType == PlanPermission.PermissionType.VIEW) {
                accessiblePlanIds.add(perm.getPlanId());
            }
        }

        List<Plan> publicPlans = planRepository.findByIsActiveTrue().stream()
                .filter(p -> isPublicPlan(p))
                .toList();
        for (Plan plan : publicPlans) {
            accessiblePlanIds.add(plan.getId());
        }

        return new ArrayList<>(accessiblePlanIds);
    }
}