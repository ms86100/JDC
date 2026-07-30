package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ExecutionReplaySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionReplaySessionRepository extends JpaRepository<ExecutionReplaySession, UUID> {

    Optional<ExecutionReplaySession> findByExecutionId(UUID executionId);
}