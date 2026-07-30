package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowTransitionTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTransitionTriggerRepository extends JpaRepository<WorkflowTransitionTrigger, UUID> {

    List<WorkflowTransitionTrigger> findByTransitionId(UUID transitionId);

    @Query("SELECT wt FROM WorkflowTransitionTrigger wt WHERE wt.transitionId = :transitionId AND wt.isEnabled = true")
    List<WorkflowTransitionTrigger> findByTransitionIdAndEnabledTrue(UUID transitionId);

    List<WorkflowTransitionTrigger> findByTriggerType(String triggerType);

    @Query("SELECT wt FROM WorkflowTransitionTrigger wt WHERE wt.isEnabled = true")
    List<WorkflowTransitionTrigger> findEnabledTriggers();

    @Query("SELECT wt FROM WorkflowTransitionTrigger wt WHERE wt.triggerType = :type AND wt.isEnabled = :enabled")
    List<WorkflowTransitionTrigger> findByTypeAndEnabled(String type, Boolean enabled);

    void deleteByTransitionId(UUID transitionId);

    long countByTransitionId(UUID transitionId);

    @Query("SELECT wt FROM WorkflowTransitionTrigger wt WHERE wt.isEnabled = true AND wt.triggerType = :triggerType")
    List<WorkflowTransitionTrigger> findEnabledByTriggerType(String triggerType);
}