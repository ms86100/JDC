package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowPostFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowPostFunctionRepository extends JpaRepository<WorkflowPostFunction, UUID> {
    List<WorkflowPostFunction> findByTransitionIdOrderBySequenceAsc(UUID transitionId);
    void deleteByTransitionId(UUID transitionId);
}