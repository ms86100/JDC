package com.jira.test.repository;

import com.jira.test.entity.ExecutionTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionTimelineEventRepository extends JpaRepository<ExecutionTimelineEvent, UUID> {

    List<ExecutionTimelineEvent> findByExecutionIdOrderBySequenceOrderAsc(UUID executionId);

    List<ExecutionTimelineEvent> findByExecutionIdAndStepIndexOrderByEventTimestampAsc(UUID executionId, Integer stepIndex);
}