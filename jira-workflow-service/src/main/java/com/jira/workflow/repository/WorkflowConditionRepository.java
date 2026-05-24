package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowConditionRepository extends JpaRepository<WorkflowCondition, UUID> {
    List<WorkflowCondition> findByTransitionId(UUID transitionId);
    List<WorkflowCondition> findByTransitionIdOrderBySequenceAsc(UUID transitionId);
    void deleteByTransitionId(UUID transitionId);
}