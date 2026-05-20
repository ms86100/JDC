package com.jira.issue.repository;

import com.jira.issue.entity.TestExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestExecutionHistoryRepository extends JpaRepository<TestExecutionHistory, UUID> {

    List<TestExecutionHistory> findByTestIssueIdOrderByExecutedAtDesc(UUID testIssueId);

    List<TestExecutionHistory> findByExecutionId(UUID executionId);

    @Query("SELECT teh FROM TestExecutionHistory teh WHERE teh.testIssueId = :testId AND teh.status = :status ORDER BY teh.executedAt DESC")
    List<TestExecutionHistory> findByTestIssueIdAndStatus(@Param("testId") UUID testIssueId, @Param("status") String status);

    @Query("SELECT teh FROM TestExecutionHistory teh WHERE teh.testIssueId = :testId AND teh.executedAt >= :start AND teh.executedAt <= :end")
    List<TestExecutionHistory> findByTestIssueIdAndDateRange(@Param("testId") UUID testIssueId,
                                                            @Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);

    @Query("SELECT teh FROM TestExecutionHistory teh WHERE teh.executedBy = :userId ORDER BY teh.executedAt DESC")
    List<TestExecutionHistory> findByExecutedBy(@Param("userId") UUID userId);

    @Query("SELECT teh FROM TestExecutionHistory teh WHERE teh.testIssueId = :testId ORDER BY teh.executedAt DESC LIMIT 1")
    Optional<TestExecutionHistory> findLatestByTestIssueId(@Param("testId") UUID testIssueId);

    @Query("SELECT COUNT(teh) FROM TestExecutionHistory teh WHERE teh.testIssueId = :testId AND teh.status = 'PASSED'")
    Long countPassedByTestIssueId(@Param("testId") UUID testIssueId);

    @Query("SELECT COUNT(teh) FROM TestExecutionHistory teh WHERE teh.testIssueId = :testId AND teh.status = 'FAILED'")
    Long countFailedByTestIssueId(@Param("testId") UUID testIssueId);
}