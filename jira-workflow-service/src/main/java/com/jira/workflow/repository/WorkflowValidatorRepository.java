package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowValidator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowValidatorRepository extends JpaRepository<WorkflowValidator, UUID> {
    List<WorkflowValidator> findByTransitionIdOrderBySequenceAsc(UUID transitionId);
    void deleteByTransitionId(UUID transitionId);
}