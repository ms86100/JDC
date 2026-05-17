package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowScheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowSchemeRepository extends JpaRepository<WorkflowScheme, UUID> {
    Optional<WorkflowScheme> findByName(String name);
    List<WorkflowScheme> findByIsDefault(Boolean isDefault);
    Optional<WorkflowScheme> findByIsDefaultTrue();
    boolean existsByName(String name);
}