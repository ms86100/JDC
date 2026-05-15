package com.jira.admin.repository;

import com.jira.admin.entity.PermissionSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PermissionSchemeRepository extends JpaRepository<PermissionSchemeEntity, String> {
    Optional<PermissionSchemeEntity> findByName(String name);
}