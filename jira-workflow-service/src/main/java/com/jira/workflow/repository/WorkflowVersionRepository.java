package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {
    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberDesc(UUID workflowId);
    Optional<WorkflowVersion> findByWorkflowIdAndVersionNumber(UUID workflowId, Integer versionNumber);

    @Query("SELECT MAX(wv.versionNumber) FROM WorkflowVersion wv WHERE wv.workflow.id = :workflowId")
    Optional<Integer> findMaxVersionNumber(UUID workflowId);

    List<WorkflowVersion> findByWorkflowId(UUID workflowId);
}