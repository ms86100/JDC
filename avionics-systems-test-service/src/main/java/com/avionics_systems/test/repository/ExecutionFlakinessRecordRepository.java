package com.avionics_systems.test.repository;

import com.avionics_systems.test.entity.ExecutionFlakinessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionFlakinessRecordRepository extends JpaRepository<ExecutionFlakinessRecord, UUID> {

    List<ExecutionFlakinessRecord> findByTestId(UUID testId);

    List<ExecutionFlakinessRecord> findByExecutionId(UUID executionId);

    @Query("SELECT e FROM ExecutionFlakinessRecord e WHERE e.testId = :testId AND e.analyzedAt >= :since ORDER BY e.analyzedAt DESC")
    List<ExecutionFlakinessRecord> findRecentByTestId(@Param("testId") UUID testId, @Param("since") LocalDateTime since);

    @Query("SELECT e FROM ExecutionFlakinessRecord e WHERE e.isFlakyExecution = true AND e.analyzedAt >= :since")
    List<ExecutionFlakinessRecord> findFlakyExecutionsSince(@Param("since") LocalDateTime since);
}