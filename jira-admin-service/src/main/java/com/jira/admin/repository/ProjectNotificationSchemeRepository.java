package com.jira.admin.repository;

import com.jira.admin.entity.ProjectNotificationSchemeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectNotificationSchemeRepository extends JpaRepository<ProjectNotificationSchemeEntity, String> {

    Optional<ProjectNotificationSchemeEntity> findByProjectId(String projectId);
}