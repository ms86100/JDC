package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.PermissionSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PermissionSchemeRepository extends JpaRepository<PermissionSchemeEntity, String> {
    Optional<PermissionSchemeEntity> findByName(String name);
    Optional<PermissionSchemeEntity> findByIsDefaultTrue();
    Optional<PermissionSchemeEntity> findFirstByIsDefaultTrue();
}