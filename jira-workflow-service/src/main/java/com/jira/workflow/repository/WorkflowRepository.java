package com.jira.workflow.repository;

import com.jira.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

    List<Workflow> findByProjectId(UUID projectId);

    Optional<Workflow> findByProjectIdAndIsDefaultTrue(UUID projectId);
}