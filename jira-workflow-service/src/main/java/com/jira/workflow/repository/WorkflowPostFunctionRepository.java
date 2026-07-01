package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowPostFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Bulk find post-functions for many transitions in a single query.
     */
    @Query("SELECT p FROM WorkflowPostFunction p WHERE p.transitionId IN :transitionIds ORDER BY p.transitionId, p.sequence ASC")
    List<WorkflowPostFunction> findByTransitionIdsOrderBySequenceAsc(@Param("transitionIds") Collection<UUID> transitionIds);
}