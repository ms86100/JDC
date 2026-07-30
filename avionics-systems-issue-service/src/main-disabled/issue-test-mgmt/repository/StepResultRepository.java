package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.StepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StepResultRepository extends JpaRepository<StepResult, UUID> {

    List<StepResult> findByExecutionIdOrderByStepOrderAsc(UUID executionId);

    List<StepResult> findByIssueIdOrderByStepOrderAsc(UUID issueId);

    Optional<StepResult> findByExecutionIdAndIssueIdAndStepOrder(UUID executionId, UUID issueId, Integer stepOrder);

    @Query("SELECT sr FROM StepResult sr WHERE sr.executionId = :executionId AND sr.status = :status")
    List<StepResult> findByExecutionIdAndStatus(@Param("executionId") UUID executionId, @Param("status") String status);

    @Query("SELECT sr FROM StepResult sr WHERE sr.defectKey IS NOT NULL AND sr.executionId = :executionId")
    List<StepResult> findFailedWithDefects(@Param("executionId") UUID executionId);

    @Query("SELECT COUNT(sr) FROM StepResult sr WHERE sr.executionId = :executionId AND sr.status = :status")
    Long countByExecutionIdAndStatus(@Param("executionId") UUID executionId, @Param("status") String status);

    @Query("SELECT AVG(sr.executionTimeMs) FROM StepResult sr WHERE sr.executionId = :executionId AND sr.executionTimeMs IS NOT NULL")
    Double findAverageExecutionTime(@Param("executionId") UUID executionId);

    void deleteByExecutionId(UUID executionId);
}