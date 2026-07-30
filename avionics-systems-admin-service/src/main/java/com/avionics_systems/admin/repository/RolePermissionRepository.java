package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, String> {

    List<RolePermissionEntity> findByProjectRoleId(String projectRoleId);

    boolean existsByProjectRoleIdAndPermissionId(String projectRoleId, String permissionId);

    @Query("SELECT rp.permissionId FROM RolePermissionEntity rp WHERE rp.projectRoleId = :roleId")
    List<String> findPermissionIdsByRoleId(@Param("roleId") String roleId);

    Optional<RolePermissionEntity> findByProjectRoleIdAndPermissionId(String projectRoleId, String permissionId);
}