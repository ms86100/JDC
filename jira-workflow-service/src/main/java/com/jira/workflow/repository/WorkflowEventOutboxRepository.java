package com.jira.workflow.repository;

import com.jira.workflow.entity.WorkflowEventOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowEventOutboxRepository extends JpaRepository<WorkflowEventOutbox, UUID> {
    List<WorkflowEventOutbox> findByPublishedFalseOrderByCreatedAtAsc();
}
