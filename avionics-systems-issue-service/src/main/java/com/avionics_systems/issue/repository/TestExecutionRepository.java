package com.avionics_systems.issue.repository;

import com.avionics_systems.issue.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    List<TestExecution> findByProjectIdOrderByStartedAtDesc(UUID projectId);

    List<TestExecution> findByTestPlanId(UUID testPlanId);

    List<TestExecution> findByTestSetId(UUID testSetId);

    List<TestExecution> findByTestId(UUID testId);

    @Query("SELECT te FROM TestExecution te WHERE te.projectId = :projectId AND te.status = :status")
    List<TestExecution> findByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") String status);

    @Query("SELECT te FROM TestExecution te WHERE te.testerId = :testerId ORDER BY te.startedAt DESC")
    List<TestExecution> findByTesterId(@Param("testerId") UUID testerId);

    @Query("SELECT te FROM TestExecution te WHERE te.ciBuildUrl = :buildUrl")
    List<TestExecution> findByCiBuildUrl(@Param("buildUrl") String ciBuildUrl);

    @Query("SELECT te FROM TestExecution te WHERE te.projectId = :projectId AND te.startedAt >= :start AND te.startedAt <= :end")
    List<TestExecution> findByProjectIdAndDateRange(@Param("projectId") UUID projectId,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    @Query("SELECT te FROM TestExecution te WHERE te.testId IN :testIds ORDER BY te.startedAt DESC")
    List<TestExecution> findByTestIds(@Param("testIds") List<UUID> testIds);

    @Query("SELECT AVG(te.durationSeconds) FROM TestExecution te WHERE te.projectId = :projectId AND te.durationSeconds IS NOT NULL")
    Double findAverageDurationByProject(@Param("projectId") UUID projectId);
}