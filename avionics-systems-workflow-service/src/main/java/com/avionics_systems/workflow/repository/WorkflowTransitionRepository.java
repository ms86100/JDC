package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    List<WorkflowTransition> findByWorkflowId(UUID workflowId);

    Optional<WorkflowTransition> findByWorkflowIdAndFromStatusIdAndToStatusId(
            UUID workflowId, UUID fromStatusId, UUID toStatusId);

    List<WorkflowTransition> findByWorkflowIdAndFromStatusId(UUID workflowId, UUID fromStatusId);

    void deleteByWorkflowId(UUID workflowId);
}