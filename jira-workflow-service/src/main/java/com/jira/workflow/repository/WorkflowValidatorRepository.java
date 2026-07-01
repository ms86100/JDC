package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowValidator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowValidatorRepository extends JpaRepository<WorkflowValidator, UUID> {
    List<WorkflowValidator> findByTransitionIdOrderBySequenceAsc(UUID transitionId);
    void deleteByTransitionId(UUID transitionId);

    @Query("SELECT v FROM WorkflowValidator v WHERE v.transitionId IN :transitionIds ORDER BY v.transitionId, v.sequence ASC")
    List<WorkflowValidator> findByTransitionIdsOrderBySequenceAsc(@Param("transitionIds") Collection<UUID> transitionIds);
}