package com.jira.admin.repository;

import com.jira.admin.entity.ProjectPrioritySchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPrioritySchemeRepository extends JpaRepository<ProjectPrioritySchemeEntity, String> {
    Optional<ProjectPrioritySchemeEntity> findByProjectId(String projectId);
    List<ProjectPrioritySchemeEntity> findBySchemeId(String schemeId);
}
