package com.jira.plan.repository;

import com.jira.plan.entity.PlanPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanPermissionRepository extends JpaRepository<PlanPermission, UUID> {

    List<PlanPermission> findByPlanId(UUID planId);

    @Query("SELECT pp FROM PlanPermission pp WHERE pp.planId = :planId AND pp.permissionType = :type")
    List<PlanPermission> findByPlanIdAndPermissionType(
            @Param("planId") UUID planId,
            @Param("type") PlanPermission.PermissionType type);

    @Query("SELECT pp FROM PlanPermission pp WHERE pp.planId = :planId AND pp.principalId = :principalId")
    List<PlanPermission> findByPlanIdAndPrincipalId(
            @Param("planId") UUID planId,
            @Param("principalId") UUID principalId);

    @Query("SELECT pp FROM PlanPermission pp WHERE pp.planId = :planId AND pp.principalType = :type AND pp.principalId = :principalId")
    List<PlanPermission> findByPlanIdAndPrincipal(
            @Param("planId") UUID planId,
            @Param("type") PlanPermission.PrincipalType type,
            @Param("principalId") UUID principalId);

    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PlanPermission pp " +
           "WHERE pp.planId = :planId AND pp.permissionType = :permissionType " +
           "AND pp.principalType = :principalType AND pp.principalId = :principalId")
    boolean hasPermission(
            @Param("planId") UUID planId,
            @Param("permissionType") PlanPermission.PermissionType permissionType,
            @Param("principalType") PlanPermission.PrincipalType principalType,
            @Param("principalId") UUID principalId);

    @Query("SELECT pp FROM PlanPermission pp WHERE pp.principalType = 'USER' AND pp.principalId = :userId")
    List<PlanPermission> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT pp FROM PlanPermission pp WHERE pp.principalType = 'GROUP' AND pp.principalId IN :groupIds")
    List<PlanPermission> findByGroupIds(@Param("groupIds") List<UUID> groupIds);

    Optional<PlanPermission> findByPlanIdAndPermissionTypeAndPrincipalTypeAndPrincipalId(
            UUID planId, PlanPermission.PermissionType permissionType,
            PlanPermission.PrincipalType principalType, UUID principalId);

    void deleteByPlanId(UUID planId);

    void deleteByPlanIdAndPrincipalId(UUID planId, UUID principalId);
}