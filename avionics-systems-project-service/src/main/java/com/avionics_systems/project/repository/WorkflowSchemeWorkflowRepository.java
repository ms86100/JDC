package com.avionics_systems.project.repository;

import com.avionics_systems.project.entity.WorkflowSchemeWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowSchemeWorkflowRepository extends JpaRepository<WorkflowSchemeWorkflow, WorkflowSchemeWorkflow.WorkflowSchemeWorkflowId> {

    List<WorkflowSchemeWorkflow> findBySchemeId(UUID schemeId);

    List<WorkflowSchemeWorkflow> findBySchemeIdAndIssueTypeNameIsNull(UUID schemeId);

    List<WorkflowSchemeWorkflow> findBySchemeIdAndIssueTypeNameIsNotNull(UUID schemeId);
}