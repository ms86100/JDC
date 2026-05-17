package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowDraftRepository extends JpaRepository<WorkflowDraft, UUID> {

    List<WorkflowDraft> findByWorkflowId(UUID workflowId);

    Optional<WorkflowDraft> findByWorkflowIdAndDraftStatus(UUID workflowId, String draftStatus);

    Optional<WorkflowDraft> findByWorkflowIdAndDraftStatusActive(UUID workflowId);

    @Query("SELECT wd FROM WorkflowDraft wd WHERE wd.workflowId = :workflowId AND wd.draftStatus = 'ACTIVE'")
    Optional<WorkflowDraft> findActiveDraftByWorkflowId(UUID workflowId);

    List<WorkflowDraft> findByCreatedBy(UUID userId);

    boolean existsByWorkflowIdAndDraftStatus(UUID workflowId, String draftStatus);

    @Query("SELECT COUNT(wd) FROM WorkflowDraft wd WHERE wd.workflowId = :workflowId AND wd.draftStatus = :status")
    long countByWorkflowIdAndStatus(UUID workflowId, String status);
}