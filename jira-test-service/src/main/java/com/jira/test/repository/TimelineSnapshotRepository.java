package com.jira.test.repository;

import com.jira.test.entity.TimelineSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimelineSnapshotRepository extends JpaRepository<TimelineSnapshot, UUID> {

    List<TimelineSnapshot> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    List<TimelineSnapshot> findByExecutionIdOrderByCreatedAtDesc(UUID executionId);

    Optional<TimelineSnapshot> findBySessionIdAndId(UUID sessionId, UUID id);

    void deleteBySessionId(UUID sessionId);

    void deleteByExecutionId(UUID executionId);
}