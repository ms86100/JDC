package com.jira.migration.unit;

import com.jira.migration.config.MigrationHealthIndicator;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.repository.DlqEntryRepository;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MigrationHealthIndicator.
 * Tests health check logic for jobs, DLQ, and stuck job detection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Migration Health Indicator Tests")
class MigrationHealthIndicatorTest {

    @Mock
    private MigrationJobRepository migrationJobRepository;

    @Mock
    private EntityStatusRepository entityStatusRepository;

    @Mock
    private DlqEntryRepository dlqEntryRepository;

    private MigrationHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new MigrationHealthIndicator(
                migrationJobRepository,
                entityStatusRepository,
                dlqEntryRepository
        );
    }

    @Nested
    @DisplayName("Health Status Tests")
    class HealthStatusTests {

        @Test
        @DisplayName("Should return UP when all jobs healthy")
        void shouldReturnUpWhenAllJobsHealthy() {
            // Given
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(5))
                    ));
            when(dlqEntryRepository.countPending()).thenReturn(5L);
            when(migrationJobRepository.countByStatus("IN_PROGRESS")).thenReturn(1L);
            when(migrationJobRepository.countByStatus("FAILED")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("COMPLETED")).thenReturn(10L);
            when(migrationJobRepository.countByStatus("PENDING")).thenReturn(2L);
            when(entityStatusRepository.findByStatus(any())).thenReturn(List.of());

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
            assertThat(health.getDetails().get("dlq_pending")).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should return DOWN when stuck jobs detected")
        void shouldReturnDownWhenStuckJobsDetected() {
            // Given - Job started 45 minutes ago (stuck threshold is 30 minutes)
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(45))
                    ));
            when(dlqEntryRepository.countPending()).thenReturn(0L);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.DOWN);
            assertThat(health.getDetails().get("stuck_jobs")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return DEGRADED when DLQ backlog is high")
        void shouldReturnDegradedWhenDlqBacklogIsHigh() {
            // Given
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of()); // No stuck jobs
            when(dlqEntryRepository.countPending()).thenReturn(150L); // High DLQ count
            when(migrationJobRepository.countByStatus("IN_PROGRESS")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("FAILED")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("COMPLETED")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("PENDING")).thenReturn(0L);
            when(entityStatusRepository.findByStatus(any())).thenReturn(List.of());

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
            assertThat(health.getDetails().get("dlq_pending")).isEqualTo(150L);
            assertThat(health.getDetails()).containsKey("recommendation");
        }

        @Test
        @DisplayName("Should report recent failures in health details")
        void shouldReportRecentFailuresInHealthDetails() {
            // Given
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of());
            when(dlqEntryRepository.countPending()).thenReturn(5L);
            when(migrationJobRepository.countByStatus("IN_PROGRESS")).thenReturn(1L);
            when(migrationJobRepository.countByStatus("FAILED")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("COMPLETED")).thenReturn(0L);
            when(migrationJobRepository.countByStatus("PENDING")).thenReturn(0L);

            // Entity with recent failure (within 24 hours)
            EntityStatus recentFailure = EntityStatus.builder()
                    .id(UUID.randomUUID())
                    .status("FAILED")
                    .completedAt(LocalDateTime.now().minusHours(12))
                    .build();
            when(entityStatusRepository.findByStatus(EntityStatus.Status.FAILED))
                    .thenReturn(List.of(recentFailure));

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getDetails()).containsKey("recent_failures_24h");
        }
    }

    @Nested
    @DisplayName("Stuck Job Detection Tests")
    class StuckJobDetectionTests {

        @Test
        @DisplayName("Should not detect job as stuck if within threshold")
        void shouldNotDetectJobAsStuckIfWithinThreshold() {
            // Given - Job started 10 minutes ago (threshold is 30 minutes)
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(10))
                    ));
            when(dlqEntryRepository.countPending()).thenReturn(0L);
            when(migrationJobRepository.countByStatus(any())).thenReturn(0L);
            when(entityStatusRepository.findByStatus(any())).thenReturn(List.of());

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
        }

        @Test
        @DisplayName("Should detect multiple stuck jobs")
        void shouldDetectMultipleStuckJobs() {
            // Given
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(60)),
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(120)),
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(180))
                    ));
            when(dlqEntryRepository.countPending()).thenReturn(0L);

            // When
            Health health = healthIndicator.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.DOWN);
            assertThat(health.getDetails().get("stuck_jobs")).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should not detect stuck job if startedAt is null")
        void shouldNotDetectStuckJobIfStartedAtIsNull() {
            // Given - Job in IN_PROGRESS but no start time (not yet started)
            MigrationJob job = MigrationJob.builder()
                    .id(UUID.randomUUID())
                    .jobStatus("IN_PROGRESS")
                    .startedAt(null) // Not started yet
                    .build();
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(job));
            when(dlqEntryRepository.countPending()).thenReturn(0L);
            when(migrationJobRepository.countByStatus(any())).thenReturn(0L);
            when(entityStatusRepository.findByStatus(any())).thenReturn(List.of());

            // When
            Health health = healthIndicator.health();

            // Then - Should not count as stuck since no start time
            assertThat(health.getStatus()).isEqualTo(org.springframework.boot.actuate.health.Status.UP);
        }
    }

    @Nested
    @DisplayName("Detailed Health Tests")
    class DetailedHealthTests {

        @Test
        @DisplayName("Should return detailed health information")
        void shouldReturnDetailedHealthInformation() {
            // Given
            when(dlqEntryRepository.countPending()).thenReturn(10L);
            when(migrationJobRepository.countByStatus("IN_PROGRESS")).thenReturn(2L);
            when(migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS"))
                    .thenReturn(List.of(
                            createJob(UUID.randomUUID(), "IN_PROGRESS", LocalDateTime.now().minusMinutes(5))
                    ));

            // When
            MigrationHealthIndicator.HealthDetails details = healthIndicator.getDetailedHealth();

            // Then
            assertThat(details.dlqPending()).isEqualTo(10L);
            assertThat(details.activeJobs()).isEqualTo(2L);
            assertThat(details.stuckJobs()).isEqualTo(0L);
        }
    }

    // Helper method to create test jobs
    private MigrationJob createJob(UUID id, String status, LocalDateTime startedAt) {
        return MigrationJob.builder()
                .id(id)
                .jobType("IMPORT")
                .jobStatus(status)
                .startedAt(startedAt)
                .initiatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }
}