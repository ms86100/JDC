package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowMigration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowMigrationRepository extends JpaRepository<WorkflowMigration, UUID> {

    List<WorkflowMigration> findByWorkflowId(UUID workflowId);

    Page<WorkflowMigration> findByWorkflowId(UUID workflowId, Pageable pageable);

    List<WorkflowMigration> findByMigrationStatus(String status);

    List<WorkflowMigration> findByOldStatusId(UUID oldStatusId);

    List<WorkflowMigration> findByNewStatusId(UUID newStatusId);

    List<WorkflowMigration> findByMigrationStatusAndWorkflowId(String status, UUID workflowId);

    long countByWorkflowIdAndMigrationStatus(UUID workflowId, String status);

    long countByWorkflowId(UUID workflowId);
}