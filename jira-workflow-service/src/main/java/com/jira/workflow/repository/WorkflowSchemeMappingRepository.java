package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowSchemeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowSchemeMappingRepository extends JpaRepository<WorkflowSchemeMapping, UUID> {
    List<WorkflowSchemeMapping> findBySchemeId(UUID schemeId);
    List<WorkflowSchemeMapping> findByIssueTypeId(UUID issueTypeId);
    List<WorkflowSchemeMapping> findByWorkflowId(UUID workflowId);
    Optional<WorkflowSchemeMapping> findBySchemeIdAndIssueTypeId(UUID schemeId, UUID issueTypeId);
    void deleteBySchemeId(UUID schemeId);

    long countBySchemeId(UUID schemeId);
}