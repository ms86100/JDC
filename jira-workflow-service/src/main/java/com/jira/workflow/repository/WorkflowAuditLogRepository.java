package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowAuditLogRepository extends JpaRepository<WorkflowAuditLog, UUID> {
    List<WorkflowAuditLog> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);
    List<WorkflowAuditLog> findBySchemeIdOrderByCreatedAtDesc(UUID schemeId);
    List<WorkflowAuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Page<WorkflowAuditLog> findByWorkflowId(UUID workflowId, Pageable pageable);
    Page<WorkflowAuditLog> findByAction(String action, Pageable pageable);
    Page<WorkflowAuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType, Pageable pageable);
    List<WorkflowAuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Page<WorkflowAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}