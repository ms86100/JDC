package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowTransitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTransitionHistoryRepository extends JpaRepository<WorkflowTransitionHistory, UUID> {
    List<WorkflowTransitionHistory> findByIssueIdOrderByExecutedAtDesc(UUID issueId);
}
