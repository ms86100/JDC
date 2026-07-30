package com.avionics_systems.migration.batch;

import com.avionics_systems.migration.entity.MigrationJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for recording and querying job state transitions.
 * Maintains an in-memory history with optional persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobStateHistoryService {

    // In-memory storage for state transitions
    private final Map<UUID, List<StateTransition>> stateHistory = new ConcurrentHashMap<>();

    /**
     * Record a state transition for a job.
     */
    public void recordTransition(UUID jobId, JobState from, JobState to, Map<String, Object> metadata) {
        StateTransition transition = StateTransition.builder()
                .jobId(jobId)
                .fromState(from)
                .toState(to)
                .timestamp(LocalDateTime.now())
                .metadata(metadata != null ? new HashMap<>(metadata) : Collections.emptyMap())
                .build();

        stateHistory.computeIfAbsent(jobId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(transition);

        log.debug("Recorded transition for job {}: {} -> {}", jobId, from, to);
    }

    /**
     * Get the full state history for a job.
     */
    public List<StateTransition> getHistory(UUID jobId) {
        return new ArrayList<>(stateHistory.getOrDefault(jobId, Collections.emptyList()));
    }

    /**
     * Get the most recent transition for a job.
     */
    public Optional<StateTransition> getLastTransition(UUID jobId) {
        List<StateTransition> transitions = stateHistory.get(jobId);
        if (transitions == null || transitions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(transitions.get(transitions.size() - 1));
    }

    /**
     * Get time spent in each state for a job.
     */
    public Map<JobState, Long> getTimeInStates(UUID jobId) {
        List<StateTransition> transitions = getHistory(jobId);
        Map<JobState, Long> timeInStates = new EnumMap<>(JobState.class);

        if (transitions.isEmpty()) {
            return timeInStates;
        }

        for (int i = 0; i < transitions.size() - 1; i++) {
            StateTransition current = transitions.get(i);
            StateTransition next = transitions.get(i + 1);

            long durationMs = java.time.Duration.between(
                    current.getTimestamp(),
                    next.getTimestamp()
            ).toMillis();

            timeInStates.merge(current.getToState(), durationMs, Long::sum);
        }

        // Add time in current state (if job is still active)
        StateTransition last = transitions.get(transitions.size() - 1);
        if (last.getToState().isActive()) {
            long currentDuration = java.time.Duration.between(
                    last.getTimestamp(),
                    LocalDateTime.now()
            ).toMillis();
            timeInStates.merge(last.getToState(), currentDuration, Long::sum);
        }

        return timeInStates;
    }

    /**
     * Clear history for a completed job (to save memory).
     */
    public void clearHistory(UUID jobId) {
        stateHistory.remove(jobId);
        log.debug("Cleared state history for job {}", jobId);
    }

    /**
     * Get transition count for a job.
     */
    public int getTransitionCount(UUID jobId) {
        List<StateTransition> transitions = stateHistory.get(jobId);
        return transitions != null ? transitions.size() : 0;
    }

    /**
     * Check if a job went through a specific state.
     */
    public boolean hasState(UUID jobId, JobState state) {
        List<StateTransition> transitions = stateHistory.get(jobId);
        if (transitions == null) {
            return false;
        }
        return transitions.stream()
                .anyMatch(t -> t.getToState() == state || t.getFromState() == state);
    }

    /**
     * Get the number of times a job transitioned to a specific state.
     */
    public int getStateVisitCount(UUID jobId, JobState state) {
        List<StateTransition> transitions = stateHistory.get(jobId);
        if (transitions == null) {
            return 0;
        }
        return (int) transitions.stream()
                .filter(t -> t.getToState() == state)
                .count();
    }

    /**
     * Build a human-readable state timeline.
     */
    public String getStateTimeline(UUID jobId) {
        List<StateTransition> transitions = getHistory(jobId);
        if (transitions.isEmpty()) {
            return "No state transitions recorded";
        }

        StringBuilder timeline = new StringBuilder();
        for (int i = 0; i < transitions.size(); i++) {
            StateTransition t = transitions.get(i);
            timeline.append(String.format("[%s] %s -> %s",
                    t.getTimestamp().toLocalTime().toString().substring(0, 8),
                    t.getFromState(),
                    t.getToState()));

            if (i < transitions.size() - 1) {
                StateTransition next = transitions.get(i + 1);
                long durationMs = java.time.Duration.between(t.getTimestamp(), next.getTimestamp()).toMillis();
                timeline.append(String.format(" (+%dms)", durationMs));
            }
            timeline.append("\n");
        }
        return timeline.toString();
    }

    /**
     * State transition record.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StateTransition {
        private UUID jobId;
        private JobState fromState;
        private JobState toState;
        private LocalDateTime timestamp;
        private Map<String, Object> metadata;
    }
}
