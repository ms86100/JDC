package com.jira.admin.repository;

import com.jira.admin.entity.ProjectRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRoleEntity, String> {
    Optional<ProjectRoleEntity> findByName(String name);
}