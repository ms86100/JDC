package com.jira.admin.repository;

import com.jira.admin.entity.MasterPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterPermissionRepository extends JpaRepository<MasterPermissionEntity, UUID> {

    Optional<MasterPermissionEntity> findByPermissionKey(String permissionKey);

    List<MasterPermissionEntity> findByIsActiveTrueOrderByCategoryAscDisplayNameAsc();

    List<MasterPermissionEntity> findByCategoryOrderByDisplayNameAsc(String category);

    boolean existsByPermissionKey(String permissionKey);
}
