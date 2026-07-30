package com.avionics_systems.workflow.repository;

import com.avionics_systems.workflow.entity.WorkflowVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersion, UUID> {

    @Query("SELECT wv FROM WorkflowVersion wv WHERE wv.workflow.id = :workflowId ORDER BY wv.versionNumber DESC")
    List<WorkflowVersion> findByWorkflowIdOrderByVersionNumberDesc(@Param("workflowId") UUID workflowId);

    @Query("SELECT wv FROM WorkflowVersion wv WHERE wv.workflow.id = :workflowId AND wv.versionNumber = :versionNumber")
    Optional<WorkflowVersion> findByWorkflowIdAndVersionNumber(
            @Param("workflowId") UUID workflowId, @Param("versionNumber") Integer versionNumber);

    @Query("SELECT MAX(wv.versionNumber) FROM WorkflowVersion wv WHERE wv.workflow.id = :workflowId")
    Optional<Integer> findMaxVersionNumber(@Param("workflowId") UUID workflowId);

    @Query("SELECT wv FROM WorkflowVersion wv WHERE wv.workflow.id = :workflowId")
    List<WorkflowVersion> findByWorkflowId(@Param("workflowId") UUID workflowId);
}