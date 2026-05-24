package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowPostFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowPostFunctionRepository extends JpaRepository<WorkflowPostFunction, UUID> {

    /**
     * Find all post-functions for a transition ordered by sequence.
     */
    List<WorkflowPostFunction> findByTransitionIdOrderBySequenceAsc(UUID transitionId);

    /**
     * Delete all post-functions for a transition.
     */
    void deleteByTransitionId(UUID transitionId);

    /**
     * Find post-functions by transition ID.
     */
    List<WorkflowPostFunction> findByTransitionId(UUID transitionId);
}