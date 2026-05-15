package com.jira.project.repository;

import com.jira.project.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, UUID> {

    List<ProjectRole> findByProjectId(UUID projectId);

    Optional<ProjectRole> findByProjectIdAndName(UUID projectId, String name);

    Optional<ProjectRole> findByName(String name);
}