package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowTransitionTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTransitionTriggerRepository extends JpaRepository<WorkflowTransitionTrigger, UUID> {

    List<WorkflowTransitionTrigger> findByTransitionId(UUID transitionId);

    @org.springframework.data.jpa.repository.Query("SELECT wt FROM WorkflowTransitionTrigger wt WHERE wt.transitionId = :transitionId AND wt.isEnabled = true")
    List<WorkflowTransitionTrigger> findByTransitionIdAndEnabledTrue(UUID transitionId);

    List<WorkflowTransitionTrigger> findByTriggerType(String triggerType);

    void deleteByTransitionId(UUID transitionId);

    long countByTransitionId(UUID transitionId);
}