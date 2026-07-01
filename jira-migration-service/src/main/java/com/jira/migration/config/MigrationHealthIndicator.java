package com.jira.migration.config;

import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.repository.DlqEntryRepository;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Health indicators for the Migration Service.
 * Provides monitoring for jobs, DLQ, and system health.
 */
@Component("migrationHealth")
@RequiredArgsConstructor
@Slf4j
public class MigrationHealthIndicator implements HealthIndicator {

    private final MigrationJobRepository migrationJobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final DlqEntryRepository dlqEntryRepository;

    // Track stuck jobs detection
    private static final long STUCK_JOB_THRESHOLD_MINUTES = 30;

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Check for stuck jobs
            long stuckJobs = countStuckJobs();
            if (stuckJobs > 0) {
                builder.down()
                       .withDetail("stuck_jobs", stuckJobs)
                       .withDetail("stuck_threshold_minutes", STUCK_JOB_THRESHOLD_MINUTES);
                return builder.build();
            }

            // Check DLQ health
            long dlqPending = dlqEntryRepository.countPending();
            if (dlqPending > 100) {
                builder.status("DEGRADED")
                       .withDetail("dlq_pending", dlqPending)
                       .withDetail("recommendation", "DLQ backlog growing, investigate failed operations");
            } else {
                builder.withDetail("dlq_pending", dlqPending);
            }

            // Check active jobs
            long activeJobs = migrationJobRepository.countByStatus("IN_PROGRESS");
            long pendingJobs = migrationJobRepository.countByStatus("PENDING");
            long failedJobs = migrationJobRepository.countByStatus("FAILED");
            long completedJobs = migrationJobRepository.countByStatus("COMPLETED");

            builder.withDetail("jobs", Map.of(
                    "active", activeJobs,
                    "pending", pendingJobs,
                    "failed", failedJobs,
                    "completed", completedJobs
            ));

            // Check for recent failures
            long recentFailures = countRecentFailures();
            if (recentFailures > 10) {
                builder.withDetail("recent_failures_24h", recentFailures);
            }

            builder.up();
            return builder.build();

        } catch (Exception e) {
            log.error("Health check failed", e);
            return builder.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private long countStuckJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STUCK_JOB_THRESHOLD_MINUTES);
        return migrationJobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS")
                .stream()
                .filter(job -> job.getStartedAt() != null && job.getStartedAt().isBefore(cutoff))
                .count();
    }

    private long countRecentFailures() {
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        return entityStatusRepository.findByStatus("FAILED")
                .stream()
                .filter(e -> e.getCompletedAt() != null && e.getCompletedAt().isAfter(dayAgo))
                .count();
    }

    // Additional detail for detailed health page
    public HealthDetails getDetailedHealth() {
        long dlqPending = dlqEntryRepository.countPending();
        long activeJobs = migrationJobRepository.countByStatus("IN_PROGRESS");

        return new HealthDetails(
                dlqPending,
                activeJobs,
                countStuckJobs(),
                migrationJobRepository.countByStatus("FAILED"),
                migrationJobRepository.countByStatus("COMPLETED"),
                migrationJobRepository.countByStatus("PENDING")
        );
    }

    public record HealthDetails(
            long dlqPending,
            long activeJobs,
            long stuckJobs,
            long failedJobs,
            long completedJobs,
            long pendingJobs
    ) {}
}