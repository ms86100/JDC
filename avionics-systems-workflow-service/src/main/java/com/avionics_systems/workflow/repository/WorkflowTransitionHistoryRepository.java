package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowTransitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTransitionHistoryRepository extends JpaRepository<WorkflowTransitionHistory, UUID> {
    List<WorkflowTransitionHistory> findByIssueIdOrderByExecutedAtDesc(UUID issueId);
    List<WorkflowTransitionHistory> findByWorkflowId(UUID workflowId);
    List<WorkflowTransitionHistory> findByWorkflowIdAndExecutedAtBetween(UUID workflowId, LocalDateTime start, LocalDateTime end);
}
