package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {

    List<WorkflowStatus> findByWorkflowIdOrderBySequenceAsc(UUID workflowId);

    boolean existsByWorkflowIdAndStatusId(UUID workflowId, UUID statusId);

    void deleteByWorkflowId(UUID workflowId);
}