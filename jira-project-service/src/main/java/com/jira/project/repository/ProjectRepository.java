package com.jira.project.repository;

import com.jira.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByProjectKey(String projectKey);

    boolean existsByProjectKey(String projectKey);
}