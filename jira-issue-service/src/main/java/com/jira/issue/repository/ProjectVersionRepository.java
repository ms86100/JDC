package com.jira.issue.repository;

import com.jira.issue.entity.ProjectVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectVersionRepository extends JpaRepository<ProjectVersion, UUID> {
    List<ProjectVersion> findByProjectIdOrderBySortOrderAsc(UUID projectId);
    List<ProjectVersion> findByProjectIdAndIsReleased(UUID projectId, Boolean isReleased);
    Optional<ProjectVersion> findByProjectIdAndName(UUID projectId, String name);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}