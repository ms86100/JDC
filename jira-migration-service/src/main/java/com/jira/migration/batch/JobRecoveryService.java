package com.jira.migration.batch;

import com.jira.migration.config.JobRecoveryConfig;
import com.jira.migration.entity.EntityStatus;
import com.jira.migration.entity.MigrationJob;
import com.jira.migration.exception.MigrationException;
import com.jira.migration.repository.EntityStatusRepository;
import com.jira.migration.repository.MigrationJobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for recovering interrupted jobs on startup and handling stale job cleanup.
 * Implements job state recovery and orphaned resource cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobRecoveryService {

    private final JobRecoveryConfig recoveryConfig;
    private final MigrationJobRepository jobRepository;
    private final EntityStatusRepository entityStatusRepository;
    private final BatchProcessingService batchProcessingService;
    private final DeadLetterQueueService dlqService;

    // Track recovery operations
    private final Set<String> recoveredJobIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Recover interrupted jobs on application startup.
     */
    @PostConstruct
    public void recoverInterruptedJobs() {
        if (!recoveryConfig.isEnabled() || !recoveryConfig.isCheckInterruptedOnStartup()) {
            log.info("Job recovery is disabled, skipping startup recovery");
            return;
        }

        log.info("Starting job recovery check on startup");

        try {
            // Find jobs that were in progress but may have been interrupted
            List<MigrationJob> interruptedJobs = findInterruptedJobs();

            if (interruptedJobs.isEmpty()) {
                log.info("No interrupted jobs found");
                return;
            }

            log.info("Found {} interrupted jobs to recover", interruptedJobs.size());

            for (MigrationJob job : interruptedJobs) {
                try {
                    JobRecoveryResult result = recoverJob(job);

                    if (result.isRecoverySuccessful()) {
                        recoveredJobIds.add(job.getId().toString());
                        log.info("Successfully recovered job {}: {}", job.getId(), result.getMessage());
                    } else {
                        log.warn("Could not recover job {}: {}", job.getId(), result.getMessage());
                    }
                } catch (Exception e) {
                    log.error("Error recovering job {}: {}", job.getId(), e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Error during job recovery: {}", e.getMessage(), e);
        }
    }

    /**
     * Find all jobs that appear to have been interrupted.
     */
    public List<MigrationJob> findInterruptedJobs() {
        // Find jobs marked as IN_PROGRESS that haven't been updated recently
        List<MigrationJob> inProgressJobs = jobRepository.findByJobStatusOrderByInitiatedAtDesc("IN_PROGRESS");

        return inProgressJobs.stream()
                .filter(job -> isJobStuck(job))
                .collect(Collectors.toList());
    }

    /**
     * Check if a job appears to be stuck (no updates for extended period).
     */
    private boolean isJobStuck(MigrationJob job) {
        if (job.getStartedAt() == null) {
            return false;
        }

        Duration elapsed = Duration.between(job.getStartedAt(), LocalDateTime.now());
        long thresholdMs = recoveryConfig.getStaleThresholdMs();

        // Job is stuck if it's been running for longer than the threshold
        return elapsed.toMillis() > thresholdMs;
    }

    /**
     * Recover a specific job.
     */
    @Transactional
    public JobRecoveryResult recoverJob(MigrationJob job) {
        log.info("Attempting to recover job: {}", job.getId());

        JobRecoveryResult result = JobRecoveryResult.builder()
                .jobId(job.getId())
                .build();

        try {
            // Determine recovery strategy based on job state
            JobState currentState = determineJobState(job);

            switch (currentState) {
                case VALIDATING:
                case MAPPING:
                case IMPORTING:
                case INDEXING:
                    // Try to resume from last checkpoint
                    return resumeJob(job.getId().toString());

                case VALIDATION_COMPLETE:
                case MAPPING_COMPLETE:
                    // Resume from that stage
                    return resumeFromStage(job, currentState);

                case FAILED:
                    // Check if it can be retried
                    if (recoveryConfig.isRecoverFailedBatches()) {
                        return retryFailedBatches(job);
                    }
                    result.setMessage("Job is in FAILED state, manual intervention required");
                    return result;

                default:
                    result.setMessage("Cannot determine recovery strategy for state: " + currentState);
                    return result;
            }

        } catch (Exception e) {
            log.error("Error recovering job {}: {}", job.getId(), e.getMessage(), e);
            result.setMessage("Recovery failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Resume a job from its last checkpoint.
     */
    @Transactional
    public JobRecoveryResult resumeJob(String jobId) {
        log.info("Resuming job: {}", jobId);

        JobRecoveryResult result = JobRecoveryResult.builder()
                .jobId(UUID.fromString(jobId))
                .recoveryType("RESUME")
                .build();

        try {
            MigrationJob job = jobRepository.findById(UUID.fromString(jobId))
                    .orElseThrow(() -> new MigrationException("Job not found: " + jobId));

            // Find last successful batch
            List<EntityStatus> processedEntities = entityStatusRepository
                    .findByJobIdOrderByProcessingOrderAsc(job.getId());

            int lastSuccessfulBatch = findLastSuccessfulBatch(processedEntities);
            int totalEntities = processedEntities.size();
            int remainingEntities = totalEntities - lastSuccessfulBatch;

            result.setLastProcessedCount(lastSuccessfulBatch);
            result.setRemainingCount(remainingEntities);
            result.setMessage(String.format("Found %d processed, %d remaining", lastSuccessfulBatch, remainingEntities));

            // Re-process failed entities from DLQ
            if (recoveryConfig.isRecoverFailedBatches()) {
                List<DeadLetterQueueService.FailedOperation> failedOps = dlqService.getByJobId(jobId);
                result.setFailedCount(failedOps.size());

                if (!failedOps.isEmpty()) {
                    log.info("Retrying {} failed operations for job {}", failedOps.size(), jobId);
                    DeadLetterQueueService.RetrySummary summary = dlqService.retryAll();
                    result.setRecoveredCount(summary.getSuccessCount());
                    result.setMessage(result.getMessage() + String.format(
                            ". DLQ retry: %d succeeded, %d failed",
                            summary.getSuccessCount(), summary.getFailedCount()));
                }
            }

            // Update job status
            job.setJobStatus("IN_PROGRESS");
            job.setProcessedEntities(lastSuccessfulBatch);
            jobRepository.save(job);

            result.setRecoverySuccessful(true);

        } catch (Exception e) {
            log.error("Error resuming job {}: {}", jobId, e.getMessage(), e);
            result.setMessage("Resume failed: " + e.getMessage());
            result.setRecoverySuccessful(false);
        }

        return result;
    }

    /**
     * Force complete a stuck job.
     */
    @Transactional
    public void forceComplete(String jobId) {
        log.warn("Force completing stuck job: {}", jobId);

        MigrationJob job = jobRepository.findById(UUID.fromString(jobId))
                .orElseThrow(() -> new MigrationException("Job not found: " + jobId));

        // Update status to completed
        job.setJobStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        job.setProgressPercentage(100.0);
        jobRepository.save(job);

        log.info("Force completed job: {}", jobId);
    }

    /**
     * Force fail a stuck job.
     */
    @Transactional
    public void forceFail(String jobId, String reason) {
        log.warn("Force failing stuck job: {} - {}", jobId, reason);

        MigrationJob job = jobRepository.findById(UUID.fromString(jobId))
                .orElseThrow(() -> new MigrationException("Job not found: " + jobId));

        // Update status to failed
        job.setJobStatus("FAILED");
        job.setCompletedAt(LocalDateTime.now());
        job.setErrorMessage("Force failed: " + reason);
        jobRepository.save(job);

        log.info("Force failed job: {}", jobId);
    }

    /**
     * Clean up stale jobs older than the configured threshold.
     */
    @Scheduled(cron = "${job.recovery.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void cleanupStaleJobs() {
        if (!recoveryConfig.isEnabled()) {
            return;
        }

        log.info("Starting stale job cleanup");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(recoveryConfig.getCleanupStaleAfterHours());

        List<MigrationJob> staleJobs = jobRepository.findPendingJobs().stream()
                .filter(job -> job.getInitiatedAt() != null && job.getInitiatedAt().isBefore(cutoff))
                .collect(Collectors.toList());

        for (MigrationJob job : staleJobs) {
            try {
                if (recoveryConfig.isAutoRecover()) {
                    forceFail(job.getId().toString(), "Stale job - no activity for " +
                            recoveryConfig.getCleanupStaleAfterHours() + " hours");
                    log.info("Cleaned up stale job: {}", job.getId());
                } else {
                    log.warn("Stale job found (not auto-cleaning): {}", job.getId());
                }
            } catch (Exception e) {
                log.error("Error cleaning up stale job {}: {}", job.getId(), e.getMessage());
            }
        }

        if (!staleJobs.isEmpty()) {
            log.info("Cleaned up {} stale jobs", staleJobs.size());
        }
    }

    /**
     * Clean up orphaned resources from interrupted jobs.
     */
    @Scheduled(fixedRateString = "${job.recovery.orphan-cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupOrphanedResources() {
        if (!recoveryConfig.isEnabled() || !recoveryConfig.isCleanupOrphanedResources()) {
            return;
        }

        log.info("Starting orphaned resource cleanup");

        // Find entity statuses with no corresponding job
        // This is a simplified version - in production, you'd have proper orphan detection

        List<EntityStatus> orphanedStatuses = entityStatusRepository.findByJobIdOrderByProcessingOrderAsc(null)
                .stream()
                .filter(status -> status.getJobId() != null)
                .filter(status -> !jobRepository.existsById(status.getJobId()))
                .collect(Collectors.toList());

        if (!orphanedStatuses.isEmpty()) {
            log.warn("Found {} orphaned entity statuses", orphanedStatuses.size());

            // Delete orphaned statuses
            for (EntityStatus status : orphanedStatuses) {
                try {
                    entityStatusRepository.delete(status);
                } catch (Exception e) {
                    log.warn("Could not delete orphaned status: {}", status.getId());
                }
            }

            log.info("Deleted {} orphaned entity statuses", orphanedStatuses.size());
        }
    }

    /**
     * Get recovery statistics.
     */
    public RecoveryStatistics getStatistics() {
        List<MigrationJob> interruptedJobs = findInterruptedJobs();

        return RecoveryStatistics.builder()
                .enabled(recoveryConfig.isEnabled())
                .autoRecover(recoveryConfig.isAutoRecover())
                .staleThresholdHours(recoveryConfig.getCleanupStaleAfterHours())
                .stuckJobCount(interruptedJobs.size())
                .recoveredJobCount(recoveredJobIds.size())
                .dlqSize(dlqService.getStatistics().getTotalEntries())
                .build();
    }

    // Private helper methods

    private JobState determineJobState(MigrationJob job) {
        String status = job.getJobStatus();
        if (status == null) {
            return JobState.PENDING;
        }
        try {
            return JobState.valueOf(status);
        } catch (IllegalArgumentException e) {
            // Handle legacy status values
            return switch (status.toUpperCase()) {
                case "IN_PROGRESS" -> JobState.IMPORTING;
                case "COMPLETED" -> JobState.COMPLETED;
                case "FAILED" -> JobState.FAILED;
                default -> JobState.PENDING;
            };
        }
    }

    private JobRecoveryResult resumeFromStage(MigrationJob job, JobState stage) {
        JobRecoveryResult result = JobRecoveryResult.builder()
                .jobId(job.getId())
                .recoveryType("RESUME_FROM_" + stage.name())
                .build();

        log.info("Resuming job {} from stage {}", job.getId(), stage);

        // Update job to continue from this stage
        job.setJobStatus("IN_PROGRESS");
        jobRepository.save(job);

        result.setRecoverySuccessful(true);
        result.setMessage("Resuming from " + stage.getDisplayName());

        return result;
    }

    private JobRecoveryResult retryFailedBatches(MigrationJob job) {
        JobRecoveryResult result = JobRecoveryResult.builder()
                .jobId(job.getId())
                .recoveryType("RETRY_FAILED")
                .build();

        List<DeadLetterQueueService.FailedOperation> failedOps = dlqService.getByJobId(job.getId().toString());
        result.setFailedCount(failedOps.size());

        if (!failedOps.isEmpty()) {
            DeadLetterQueueService.RetrySummary summary = dlqService.retryAll();
            result.setRecoveredCount(summary.getSuccessCount());
            result.setMessage(String.format("Retried %d failed, %d recovered",
                    failedOps.size(), summary.getSuccessCount()));
        } else {
            result.setMessage("No failed batches to retry");
        }

        result.setRecoverySuccessful(true);

        return result;
    }

    private int findLastSuccessfulBatch(List<EntityStatus> entities) {
        int lastSuccessful = 0;

        for (EntityStatus entity : entities) {
            if ("COMPLETED".equals(entity.getStatus())) {
                lastSuccessful = Math.max(lastSuccessful,
                        entity.getProcessingOrder() != null ? entity.getProcessingOrder() + 1 : 0);
            }
        }

        return lastSuccessful;
    }

    /**
     * Result of a job recovery operation.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JobRecoveryResult {
        private UUID jobId;
        private boolean recoverySuccessful;
        private String recoveryType;
        private String message;
        private int lastProcessedCount;
        private int remainingCount;
        private int recoveredCount;
        private int failedCount;
        private UUID resumedFromCheckpoint;
    }

    /**
     * Recovery statistics.
     */
    @lombok.Data
    @lombok.Builder
    public static class RecoveryStatistics {
        private boolean enabled;
        private boolean autoRecover;
        private int staleThresholdHours;
        private int stuckJobCount;
        private int recoveredJobCount;
        private long dlqSize;
    }
}