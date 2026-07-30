package com.avionics_systems.migration.batch;

import com.avionics_systems.migration.entity.MigrationJob;
import com.avionics_systems.migration.exception.MigrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * State machine for managing job lifecycle transitions.
 * Ensures all state changes follow defined rules and are properly logged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobStateMachine {

    private final JobStateHistoryService stateHistoryService;

    /**
     * Check if a state transition is valid.
     */
    public boolean canTransition(JobState from, JobState to) {
        if (from == null || to == null) {
            return false;
        }
        return from.canTransitionTo(to);
    }

    /**
     * Transition a job to a new state, with validation.
     *
     * @param job The migration job to transition
     * @param newState The target state
     * @return The job with updated state
     * @throws MigrationException if the transition is not valid
     */
    public MigrationJob transition(MigrationJob job, JobState newState) {
        return transition(job, newState, null);
    }

    /**
     * Transition a job to a new state with optional metadata.
     *
     * @param job The migration job to transition
     * @param newState The target state
     * @param metadata Optional metadata about the transition
     * @return The job with updated state
     * @throws MigrationException if the transition is not valid
     */
    public MigrationJob transition(MigrationJob job, JobState newState, Map<String, Object> metadata) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());

        if (!canTransition(currentState, newState)) {
            String error = String.format("Invalid state transition: %s -> %s (current: %s)",
                    currentState, newState, currentState);
            log.error("State transition rejected for job {}: {}", job.getId(), error);
            throw new MigrationException(error, "INVALID_STATE_TRANSITION");
        }

        JobState previousState = currentState;
        long transitionTime = System.currentTimeMillis();

        // Update job status
        job.setJobStatus(newState.name());

        // Record transition
        recordTransition(job.getId(), previousState, newState, metadata);

        // Update timestamps based on state
        updateTimestamps(job, newState);

        log.info("Job {} transitioned: {} -> {} (took {}ms in {})",
                job.getId(), previousState, newState,
                transitionTime - (job.getStartedAt() != null ?
                        job.getStartedAt().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() : 0),
                previousState);

        return job;
    }

    /**
     * Get list of valid next states from the current job state.
     */
    public List<JobState> getValidNextStates(MigrationJob job) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());
        return new ArrayList<>(currentState.getValidNextStates());
    }

    /**
     * Check if the job can be cancelled from its current state.
     */
    public boolean canCancel(MigrationJob job) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());
        return currentState.canTransitionTo(JobState.CANCELLED);
    }

    /**
     * Check if the job can be retried from its current state.
     */
    public boolean canRetry(MigrationJob job) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());
        return currentState == JobState.FAILED && currentState.canTransitionTo(JobState.PENDING);
    }

    /**
     * Get the current state of a job from its status string.
     */
    public JobState getStateFromJobStatus(String status) {
        if (status == null) {
            return JobState.PENDING;
        }
        try {
            return JobState.valueOf(status);
        } catch (IllegalArgumentException e) {
            // Handle legacy status values
            return mapLegacyStatus(status);
        }
    }

    /**
     * Map legacy status strings to new job states.
     */
    private JobState mapLegacyStatus(String legacyStatus) {
        return switch (legacyStatus.toUpperCase()) {
            case "IN_PROGRESS" -> JobState.IMPORTING;
            case "COMPLETED" -> JobState.COMPLETED;
            case "FAILED" -> JobState.FAILED;
            case "CANCELLED", "CANCELED" -> JobState.CANCELLED;
            default -> JobState.PENDING;
        };
    }

    /**
     * Validate that a job can proceed to the next logical state.
     */
    public void validateProgression(MigrationJob job) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());

        if (currentState == JobState.COMPLETED || currentState == JobState.CANCELLED) {
            throw new MigrationException(
                    String.format("Job %s is in terminal state %s and cannot progress", job.getId(), currentState),
                    "INVALID_JOB_STATE");
        }
    }

    /**
     * Record a state transition in history.
     */
    private void recordTransition(UUID jobId, JobState from, JobState to, Map<String, Object> metadata) {
        if (stateHistoryService != null) {
            stateHistoryService.recordTransition(jobId, from, to, metadata);
        }
    }

    /**
     * Update job timestamps based on state changes.
     */
    private void updateTimestamps(MigrationJob job, JobState newState) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        switch (newState) {
            case VALIDATING, MAPPING, IMPORTING, INDEXING:
                if (job.getStartedAt() == null) {
                    job.setStartedAt(now);
                }
                break;
            case COMPLETED, FAILED, CANCELLED:
                job.setCompletedAt(now);
                break;
            default:
                break;
        }
    }

    /**
     * Calculate estimated completion time based on current progress.
     */
    public long estimateCompletionTime(MigrationJob job) {
        if (job.getStartedAt() == null || job.getTotalEntities() == null || job.getTotalEntities() == 0) {
            return -1;
        }

        JobState currentState = getStateFromJobStatus(job.getJobStatus());
        if (!currentState.isActive()) {
            return -1;
        }

        long elapsedMs = java.time.Duration.between(
                job.getStartedAt(),
                java.time.LocalDateTime.now()
        ).toMillis();

        int processed = job.getProcessedEntities() != null ? job.getProcessedEntities() : 0;
        if (processed == 0) {
            return -1;
        }

        double progressRate = (double) processed / elapsedMs;
        int remaining = job.getTotalEntities() - processed;

        return (long) (remaining / progressRate);
    }

    /**
     * Get a summary of the job's state for display purposes.
     */
    public String getStateSummary(MigrationJob job) {
        JobState currentState = getStateFromJobStatus(job.getJobStatus());
        List<JobState> nextStates = getValidNextStates(job);

        StringBuilder summary = new StringBuilder();
        summary.append("State: ").append(currentState.getDisplayName());

        if (!nextStates.isEmpty()) {
            summary.append(" | Next: ");
            summary.append(nextStates.stream()
                    .map(JobState::getDisplayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }

        if (currentState.isActive() && job.getProgressPercentage() != null) {
            summary.append(String.format(" | Progress: %.1f%%", job.getProgressPercentage()));
        }

        return summary.toString();
    }
}
