package com.jira.admin.repository;

import com.jira.admin.entity.ProjectRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRoleEntity, UUID> {
    Optional<ProjectRoleEntity> findByName(String name);
}