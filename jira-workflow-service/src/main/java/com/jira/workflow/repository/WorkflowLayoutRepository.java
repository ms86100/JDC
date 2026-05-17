package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowLayoutRepository extends JpaRepository<WorkflowLayout, UUID> {

    List<WorkflowLayout> findByWorkflowId(UUID workflowId);

    Optional<WorkflowLayout> findTopByWorkflowIdOrderByLayoutVersionDesc(UUID workflowId);

    @org.springframework.data.jpa.repository.Query("SELECT wl FROM WorkflowLayout wl WHERE wl.workflowId = :workflowId AND wl.isLocked = true")
    Optional<WorkflowLayout> findByWorkflowIdAndLockedTrue(UUID workflowId);

    boolean existsByWorkflowId(UUID workflowId);

    void deleteByWorkflowId(UUID workflowId);
}