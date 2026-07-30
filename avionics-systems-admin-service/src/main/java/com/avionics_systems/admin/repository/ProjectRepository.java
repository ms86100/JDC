package com.avionics_systems.admin.repository;

import com.avionics_systems.admin.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    long countByIssueTypeScheme(String issueTypeScheme);

    java.util.Optional<ProjectEntity> findByProjectKey(String projectKey);
}