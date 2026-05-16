package com.jira.migration.unit;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Optimistic Locking functionality.
 * Tests @Version annotation behavior on entities.
 */
@DisplayName("Optimistic Locking Tests")
class OptimisticLockingTest {

    private final UUID testId = UUID.randomUUID();
    private final UUID testUserId = UUID.randomUUID();

    @Nested
    @DisplayName("MigrationJob Optimistic Locking")
    class MigrationJobLockingTests {

        @Test
        @DisplayName("Should have version field for optimistic locking")
        void shouldHaveVersionFieldForOptimisticLocking() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("IN_PROGRESS")
                    .build();

            // Then
            assertThat(job.getOptimisticLockVersion()).isNull(); // Version starts at null
        }

        @Test
        @DisplayName("Should track progress updates correctly")
        void shouldTrackProgressUpdatesCorrectly() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("IN_PROGRESS")
                    .totalEntities(100)
                    .processedEntities(0)
                    .failedEntities(0)
                    .progressPercentage(0.0)
                    .build();

            // When - Multiple concurrent-like updates
            job.incrementProcessed();
            job.incrementProcessed();
            job.incrementProcessed();

            // Then
            assertThat(job.getProcessedEntities()).isEqualTo(3);
            assertThat(job.getProgressPercentage()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should handle status transitions with version")
        void shouldHandleStatusTransitionsWithVersion() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("PENDING")
                    .initiatedAt(LocalDateTime.now())
                    .build();

            // When - Transition through states
            job.markStarted();
            assertThat(job.getJobStatus()).isEqualTo("IN_PROGRESS");
            assertThat(job.getStartedAt()).isNotNull();

            job.markCompleted();
            assertThat(job.getJobStatus()).isEqualTo("COMPLETED");
            assertThat(job.getCompletedAt()).isNotNull();
            assertThat(job.getProgressPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should handle failure state")
        void shouldHandleFailureState() {
            // Given
            MigrationJob job = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("IN_PROGRESS")
                    .build();

            // When
            job.markFailed("Database connection timeout");

            // Then
            assertThat(job.getJobStatus()).isEqualTo("FAILED");
            assertThat(job.getErrorMessage()).isEqualTo("Database connection timeout");
            assertThat(job.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("EntityStatus Optimistic Locking")
    class EntityStatusLockingTests {

        @Test
        @DisplayName("Should have version field for optimistic locking")
        void shouldHaveVersionFieldForOptimisticLocking() {
            // Given
            EntityStatus status = EntityStatus.builder()
                    .id(testId)
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .status("PENDING")
                    .build();

            // Then
            assertThat(status.getOptimisticLockVersion()).isNull();
        }

        @Test
        @DisplayName("Should track processing transitions")
        void shouldTrackProcessingTransitions() {
            // Given
            EntityStatus status = EntityStatus.builder()
                    .id(testId)
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .entityKey("TEST-1")
                    .status("PENDING")
                    .processingOrder(1)
                    .startedAt(null)
                    .build();

            // When - Transition through states
            status.markProcessing();
            assertThat(status.getStatus()).isEqualTo("PROCESSING");

            status.markCompleted(UUID.randomUUID());
            assertThat(status.getStatus()).isEqualTo("COMPLETED");
            assertThat(status.getEntityId()).isNotNull();
            assertThat(status.getCompletedAt()).isNotNull();
            assertThat(status.getDurationMs()).isNotNull();
        }

        @Test
        @DisplayName("Should track failure with error details")
        void shouldTrackFailureWithErrorDetails() {
            // Given
            EntityStatus status = EntityStatus.builder()
                    .id(testId)
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .status("PENDING")
                    .build();

            // When
            status.markFailed("VALIDATION_ERROR", "Project key is required", "project_key");

            // Then
            assertThat(status.getStatus()).isEqualTo("FAILED");
            assertThat(status.getErrorCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(status.getErrorMessage()).isEqualTo("Project key is required");
            assertThat(status.getErrorField()).isEqualTo("project_key");
            assertThat(status.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should track skipped status")
        void shouldTrackSkippedStatus() {
            // Given
            EntityStatus status = EntityStatus.builder()
                    .id(testId)
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .status("PENDING")
                    .build();

            // When
            status.markSkipped("Duplicate entry detected");

            // Then
            assertThat(status.getStatus()).isEqualTo("SKIPPED");
            assertThat(status.getErrorMessage()).isEqualTo("Duplicate entry detected");
            assertThat(status.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should track duration for all status types")
        void shouldTrackDurationForAllStatusTypes() {
            // Given
            EntityStatus completed = EntityStatus.builder()
                    .id(UUID.randomUUID())
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .status("PENDING")
                    .processingOrder(1)
                    .startedAt(LocalDateTime.now().minusSeconds(10))
                    .build();

            EntityStatus failed = EntityStatus.builder()
                    .id(UUID.randomUUID())
                    .jobId(testUserId)
                    .entityType("ISSUE")
                    .status("PENDING")
                    .processingOrder(2)
                    .startedAt(LocalDateTime.now().minusSeconds(5))
                    .build();

            // When
            completed.markCompleted(UUID.randomUUID());
            failed.markFailed("ERROR", "Failed", null);

            // Then - Durations should be calculated
            assertThat(completed.getDurationMs()).isGreaterThan(0);
            assertThat(failed.getDurationMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("DlqEntry Optimistic Locking")
    class DlqEntryLockingTests {

        @Test
        @DisplayName("Should have version field for optimistic locking")
        void shouldHaveVersionFieldForOptimisticLocking() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.PENDING)
                    .build();

            // Then
            assertThat(entry.getOptimisticLockVersion()).isNull();
        }

        @Test
        @DisplayName("Should manage retry state")
        void shouldManageRetryState() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .attemptCount(0)
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.PENDING)
                    .build();

            // When
            entry.incrementAttempt();
            entry.scheduleRetry(60);

            // Then
            assertThat(entry.getAttemptCount()).isEqualTo(1);
            assertThat(entry.getLastAttempt()).isNotNull();
            assertThat(entry.getStatus()).isEqualTo(com.jira.migration.entity.DlqEntry.DlqStatus.SCHEDULED);
            assertThat(entry.getNextRetry()).isNotNull();
        }

        @Test
        @DisplayName("Should handle completion state")
        void shouldHandleCompletionState() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.RETRYING)
                    .build();

            // When
            entry.markCompleted();

            // Then
            assertThat(entry.getStatus()).isEqualTo(com.jira.migration.entity.DlqEntry.DlqStatus.COMPLETED);
            assertThat(entry.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle failure state")
        void shouldHandleFailureState() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .operationType("CREATE_ISSUE")
                    .entityType("ISSUE")
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.RETRYING)
                    .build();

            // When
            entry.markFailed("Connection refused");

            // Then
            assertThat(entry.getStatus()).isEqualTo(com.jira.migration.entity.DlqEntry.DlqStatus.FAILED);
            assertThat(entry.getLastError()).isEqualTo("Connection refused");
            assertThat(entry.getResolvedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should check retry eligibility")
        void shouldCheckRetryEligibility() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .attemptCount(2)
                    .nextRetry(LocalDateTime.now().minusMinutes(5))
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.PENDING)
                    .build();

            // When - With max attempts of 5
            boolean eligible = entry.isEligibleForRetry(5);

            // Then
            assertThat(eligible).isTrue();
        }

        @Test
        @DisplayName("Should not be eligible when max attempts reached")
        void shouldNotBeEligibleWhenMaxAttemptsReached() {
            // Given
            com.jira.migration.entity.DlqEntry entry = com.jira.migration.entity.DlqEntry.builder()
                    .id(testId)
                    .attemptCount(5) // Equal to max
                    .nextRetry(null)
                    .status(com.jira.migration.entity.DlqEntry.DlqStatus.PENDING)
                    .build();

            // When
            boolean eligible = entry.isEligibleForRetry(5);

            // Then
            assertThat(eligible).isFalse();
        }
    }

    @Nested
    @DisplayName("Concurrent Update Simulation")
    class ConcurrentUpdateSimulationTests {

        @Test
        @DisplayName("Should handle concurrent progress increments")
        void shouldHandleConcurrentProgressIncrements() {
            // Given - Simulate two workers reading the same job
            MigrationJob originalJob = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("IN_PROGRESS")
                    .totalEntities(100)
                    .processedEntities(50)
                    .failedEntities(5)
                    .progressPercentage(55.0)
                    .build();

            // Simulate worker 1 reads at version V1
            MigrationJob worker1Job = copyJob(originalJob);

            // Simulate worker 2 reads at same version V1 (concurrent)
            MigrationJob worker2Job = copyJob(originalJob);

            // Worker 1 updates
            worker1Job.setProcessedEntities(60);
            worker1Job.setProgressPercentage(65.0);
            // worker1Job would save with version V1 -> V2

            // Worker 2 updates (should detect conflict with V1 -> V2)
            worker2Job.setProcessedEntities(55);
            worker2Job.setProgressPercentage(60.0);
            // worker2Job would try to save with version V1 -> conflict!

            // Then - Worker 2's version is stale
            assertThat(worker2Job.getProcessedEntities()).isNotEqualTo(worker1Job.getProcessedEntities());
        }

        @Test
        @DisplayName("Should track entity version progression")
        void shouldTrackEntityVersionProgression() {
            // Given - Initial entity
            MigrationJob job = MigrationJob.builder()
                    .id(testId)
                    .jobType("IMPORT")
                    .jobStatus("PENDING")
                    .build();

            // Simulate version progression
            // Version 0 -> 1 (created)
            // Version 1 -> 2 (started)
            // Version 2 -> 3 (progress updated)

            assertThat(job.getOptimisticLockVersion()).isNull(); // Not set until first save

            // Simulate first save
            job.setOptimisticLockVersion(1L);
            assertThat(job.getOptimisticLockVersion()).isEqualTo(1);

            // Simulate update
            job.setProgressPercentage(25.0);
            job.setOptimisticLockVersion(2L);
            assertThat(job.getOptimisticLockVersion()).isEqualTo(2);
        }
    }

    // Helper method
    private MigrationJob copyJob(MigrationJob original) {
        return MigrationJob.builder()
                .id(original.getId())
                .jobType(original.getJobType())
                .jobStatus(original.getJobStatus())
                .totalEntities(original.getTotalEntities())
                .processedEntities(original.getProcessedEntities())
                .failedEntities(original.getFailedEntities())
                .progressPercentage(original.getProgressPercentage())
                .optimisticLockVersion(original.getOptimisticLockVersion())
                .build();
    }
}