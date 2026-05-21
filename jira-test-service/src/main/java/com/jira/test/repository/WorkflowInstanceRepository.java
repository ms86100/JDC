package com.jira.test.repository;

import com.jira.test.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    List<WorkflowInstance> findByIsCompletedFalse();

    List<WorkflowInstance> findByDefinitionId(UUID definitionId);

    List<WorkflowInstance> findByEntityId(UUID entityId);

    List<WorkflowInstance> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<WorkflowInstance> findByAssignedTo(UUID assignedTo);

    List<WorkflowInstance> findByInitiatedBy(UUID initiatedBy);

    @Query("SELECT COUNT(w) FROM WorkflowInstance w WHERE w.definitionId = :definitionId AND w.isCompleted = false")
    long countByDefinitionIdAndIsCompletedFalse(@Param("definitionId") UUID definitionId);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.isCompleted = false ORDER BY w.createdAt DESC")
    List<WorkflowInstance> findPendingInstances();

    List<WorkflowInstance> findByDefinitionIdAndIsCompletedFalse(UUID definitionId);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.assignedTo = :userId AND w.isCompleted = false")
    List<WorkflowInstance> findActiveByAssignedTo(@Param("userId") UUID userId);

    @Query("SELECT w FROM WorkflowInstance w WHERE w.currentState = :state AND w.isCompleted = false")
    List<WorkflowInstance> findActiveByCurrentState(@Param("state") String state);
}