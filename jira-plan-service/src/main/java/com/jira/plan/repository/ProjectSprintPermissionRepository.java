package com.jira.plan.repository;

import com.jira.plan.entity.ProjectSprintPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectSprintPermissionRepository extends JpaRepository<ProjectSprintPermission, UUID> {

    List<ProjectSprintPermission> findByProjectId(UUID projectId);

    List<ProjectSprintPermission> findByProjectIdAndPermissionKey(UUID projectId, String permissionKey);

    List<ProjectSprintPermission> findByPermissionKey(String permissionKey);

    List<ProjectSprintPermission> findByPrincipalTypeAndPrincipalId(String principalType, String principalId);

    boolean existsByProjectIdAndPermissionKeyAndPrincipalId(UUID projectId, String permissionKey, String principalId);

    void deleteByProjectId(UUID projectId);
}