package com.jira.admin.repository;

import com.jira.admin.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    long countByIssueTypeScheme(String issueTypeScheme);

    java.util.Optional<ProjectEntity> findByProjectKey(String projectKey);
}