package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowSharing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowSharingRepository extends JpaRepository<WorkflowSharing, UUID> {

    List<WorkflowSharing> findByWorkflowId(UUID workflowId);

    List<WorkflowSharing> findByProjectId(UUID projectId);

    Optional<WorkflowSharing> findByWorkflowIdAndProjectId(UUID workflowId, UUID projectId);

    @Query("SELECT COUNT(ws) FROM WorkflowSharing ws WHERE ws.workflowId = :workflowId")
    long countByWorkflowId(UUID workflowId);

    @Query("SELECT COUNT(ws) FROM WorkflowSharing ws WHERE ws.projectId = :projectId")
    long countByProjectId(UUID projectId);

    boolean existsByWorkflowIdAndProjectId(UUID workflowId, UUID projectId);

    void deleteByWorkflowIdAndProjectId(UUID workflowId, UUID projectId);
}