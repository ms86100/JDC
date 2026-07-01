package com.jira.test.repository;

import com.jira.test.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, UUID> {

    List<TestRun> findByTestIdOrderByExecutedAtDesc(UUID testId);

    List<TestRun> findByProjectIdOrderByExecutedAtDesc(UUID projectId);

    List<TestRun> findByExecutionId(UUID executionId);

    List<TestRun> findByStatus(String status);

    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.status = :status ORDER BY tr.executedAt DESC")
    List<TestRun> findByTestIdAndStatus(@Param("testId") UUID testId, @Param("status") String status);

    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.executedAt >= :since ORDER BY tr.executedAt DESC")
    List<TestRun> findByTestIdSince(@Param("testId") UUID testId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.testId = :testId AND tr.status = 'PASSED'")
    long countPassedByTestId(@Param("testId") UUID testId);

    @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.testId = :testId AND tr.status = 'FAILED'")
    long countFailedByTestId(@Param("testId") UUID testId);

    @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.testId = :testId")
    long countTotalByTestId(@Param("testId") UUID testId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.executedBy = :userId ORDER BY tr.executedAt DESC")
    List<TestRun> findByExecutedBy(@Param("userId") UUID userId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.environment = :env AND tr.projectId = :projectId ORDER BY tr.executedAt DESC")
    List<TestRun> findByEnvironment(@Param("env") String env, @Param("projectId") UUID projectId);

    @Query("SELECT AVG(CAST(tr.duration AS float)) FROM TestRun tr WHERE tr.testId = :testId AND tr.duration IS NOT NULL")
    Double getAverageDurationByTestId(@Param("testId") UUID testId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId ORDER BY tr.executedAt DESC LIMIT 1")
    Optional<TestRun> findLatestByTestId(@Param("testId") UUID testId);

    // Flaky detection: runs with alternating pass/fail
    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.executedAt >= :since ORDER BY tr.executedAt ASC")
    List<TestRun> findRecentByTestId(@Param("testId") UUID testId, @Param("since") LocalDateTime since);

    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.executedAt BETWEEN :start AND :end ORDER BY tr.executedAt DESC")
    List<TestRun> findByProjectIdAndExecutedAtBetween(@Param("projectId") UUID projectId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("SELECT tr FROM TestRun tr WHERE tr.parentRunId = :parentRunId ORDER BY tr.executedAt DESC")
    List<TestRun> findRetriesByParentRunId(@Param("parentRunId") UUID parentRunId);

    // Baseline queries
    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.isBaseline = true ORDER BY tr.createdAt DESC")
    Optional<TestRun> findBaselineByTestId(@Param("testId") UUID testId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.isBaseline = true ORDER BY tr.createdAt DESC")
    List<TestRun> findBaselinesByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.isBaseline = true ORDER BY tr.createdAt DESC LIMIT 1")
    Optional<TestRun> findLatestBaselineByTestId(@Param("testId") UUID testId);

    // Tag queries
    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.tags IS NOT NULL AND :tag = ANY(tr.tags) ORDER BY tr.executedAt DESC")
    List<TestRun> findByProjectIdAndTag(@Param("projectId") UUID projectId, @Param("tag") String tag);

    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.tags IS NOT NULL AND tr.tags && CAST(:tags AS text[]) ORDER BY tr.executedAt DESC")
    List<TestRun> findByProjectIdAndAnyTag(@Param("projectId") UUID projectId, @Param("tags") List<String> tags);

    // Archive queries
    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.isArchived = false ORDER BY tr.executedAt DESC")
    List<TestRun> findActiveByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.isArchived = true ORDER BY tr.archivedAt DESC")
    List<TestRun> findArchivedByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT tr FROM TestRun tr WHERE tr.isBaseline = true ORDER BY tr.createdAt DESC")
    List<TestRun> findAllBaselines();

    // Flakiness queries
    @Query("SELECT tr FROM TestRun tr WHERE tr.projectId = :projectId AND tr.flakinessScore IS NOT NULL AND tr.flakinessScore > :threshold ORDER BY tr.flakinessScore DESC")
    List<TestRun> findFlakyByProjectId(@Param("projectId") UUID projectId, @Param("threshold") Double threshold);

    @Query("SELECT tr FROM TestRun tr WHERE tr.testId = :testId AND tr.executedAt >= :start AND tr.executedAt <= :end ORDER BY tr.executedAt ASC")
    List<TestRun> findByTestIdInDateRange(@Param("testId") UUID testId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Statistics queries
    @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(tr) FROM TestRun tr WHERE tr.projectId = :projectId AND tr.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") String status);

    @Query("SELECT AVG(CAST(tr.duration AS float)) FROM TestRun tr WHERE tr.projectId = :projectId AND tr.duration IS NOT NULL AND tr.executedAt >= :since")
    Double getAverageDurationByProjectIdSince(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);

    @Query("SELECT tr.status, COUNT(tr) FROM TestRun tr WHERE tr.projectId = :projectId AND tr.executedAt >= :since GROUP BY tr.status")
    List<Object[]> countByStatusSince(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);

    @Query("SELECT tr.environment, COUNT(tr), SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END) FROM TestRun tr WHERE tr.projectId = :projectId AND tr.executedAt >= :since AND tr.environment IS NOT NULL GROUP BY tr.environment")
    List<Object[]> countByEnvironmentSince(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);

    // Daily trends
    @Query("SELECT CAST(tr.executedAt AS LocalDate), COUNT(tr), SUM(CASE WHEN tr.status = 'PASSED' THEN 1 ELSE 0 END), SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END) FROM TestRun tr WHERE tr.projectId = :projectId AND tr.executedAt >= :since GROUP BY CAST(tr.executedAt AS LocalDate) ORDER BY CAST(tr.executedAt AS LocalDate)")
    List<Object[]> getDailyTrends(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);
}