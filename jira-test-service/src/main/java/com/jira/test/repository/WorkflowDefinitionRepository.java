package com.jira.test.repository;

import com.jira.test.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    List<WorkflowDefinition> findByProjectId(UUID projectId);

    List<WorkflowDefinition> findByIsActiveTrue();

    List<WorkflowDefinition> findByProjectIdAndIsActiveTrue(UUID projectId);

    List<WorkflowDefinition> findByWorkflowType(String workflowType);

    List<WorkflowDefinition> findByProjectIdAndWorkflowType(UUID projectId, String workflowType);

    Optional<WorkflowDefinition> findByProjectIdAndWorkflowTypeAndIsDefaultTrue(UUID projectId, String workflowType);

    List<WorkflowDefinition> findByIsDefaultTrue();

    @Query("SELECT d FROM WorkflowDefinition d WHERE d.projectId = :projectId AND d.isActive = true")
    List<WorkflowDefinition> findActiveByProject(@Param("projectId") UUID projectId);
}