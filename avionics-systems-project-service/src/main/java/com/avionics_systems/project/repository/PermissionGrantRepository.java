package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.PermissionGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PermissionGrantRepository extends JpaRepository<PermissionGrant, UUID> {
    List<PermissionGrant> findByPermissionSchemeId(UUID schemeId);
    List<PermissionGrant> findByPermissionKey(String permissionKey);
    List<PermissionGrant> findByEntityId(UUID entityId);
    List<PermissionGrant> findByGroupName(String groupName);
    List<PermissionGrant> findByProjectRoleId(UUID projectRoleId);
}