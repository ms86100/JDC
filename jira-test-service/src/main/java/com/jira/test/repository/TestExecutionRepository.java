package com.jira.test.repository;

import com.jira.test.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, UUID> {

    List<TestExecution> findByTestSetId(UUID testSetId);

    List<TestExecution> findByTestId(UUID testId);

    List<TestExecution> findByTestIdOrderByCreatedAtDesc(UUID testId);

    List<TestExecution> findByTesterId(UUID testerId);

    @Query("SELECT e FROM TestExecution e WHERE e.testSetId = :setId ORDER BY e.createdAt DESC")
    List<TestExecution> findHistoryByTestSetId(@Param("setId") UUID setId);

    @Query("SELECT e FROM TestExecution e WHERE e.testerId = :testerId AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<TestExecution> findRecentByTester(@Param("testerId") UUID testerId, @Param("since") LocalDateTime since);

    @Query("SELECT e FROM TestExecution e WHERE e.testCycle = :cycle ORDER BY e.createdAt DESC")
    List<TestExecution> findByTestCycle(@Param("cycle") String cycle);

    @Query("SELECT AVG(CAST(e.passedTests AS float) / NULLIF(e.totalTests, 0) * 100) FROM TestExecution e WHERE e.testSetId = :setId AND e.totalTests > 0")
    Double getAveragePassRateForSet(@Param("setId") UUID setId);

    @Query("SELECT e FROM TestExecution e WHERE e.createdAt >= :since")
    List<TestExecution> findByExecutedAtAfter(@Param("since") LocalDateTime since);

    boolean existsByProjectId(UUID projectId);
}