package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowStatusCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStatusCategoryRepository extends JpaRepository<WorkflowStatusCategory, UUID> {

    Optional<WorkflowStatusCategory> findByCategoryKey(String categoryKey);

    boolean existsByCategoryKey(String categoryKey);

    java.util.List<WorkflowStatusCategory> findAllByOrderBySequenceAsc();
}