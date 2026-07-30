package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, UUID> {

    List<WorkflowStatus> findByWorkflowIdOrderBySequenceAsc(UUID workflowId);

    boolean existsByWorkflowIdAndStatusId(UUID workflowId, UUID statusId);

    Optional<WorkflowStatus> findByWorkflowIdAndStatusId(UUID workflowId, UUID statusId);

    void deleteByWorkflowId(UUID workflowId);

    void deleteByWorkflowIdAndStatusId(UUID workflowId, UUID statusId);

    @Modifying
    @Query("UPDATE WorkflowStatus ws SET ws.sequence = :sequence WHERE ws.id = :id")
    void updateSequence(UUID id, Integer sequence);
}