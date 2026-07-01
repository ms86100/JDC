package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowMigrationIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowMigrationIssueRepository extends JpaRepository<WorkflowMigrationIssue, UUID> {

    List<WorkflowMigrationIssue> findByMigrationId(UUID migrationId);

    Page<WorkflowMigrationIssue> findByMigrationId(UUID migrationId, Pageable pageable);

    List<WorkflowMigrationIssue> findByMigrationIdAndMigrationStatus(UUID migrationId, String status);

    List<WorkflowMigrationIssue> findByIssueId(UUID issueId);

    long countByMigrationId(UUID migrationId);

    long countByMigrationIdAndMigrationStatus(UUID migrationId, String status);
}