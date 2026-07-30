package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.MasterRolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MasterRolePermissionRepository extends JpaRepository<MasterRolePermissionEntity, UUID> {

    List<MasterRolePermissionEntity> findByRoleId(UUID roleId);

    List<MasterRolePermissionEntity> findByPermissionId(UUID permissionId);

    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
}
