package com.avionics_systems.migration.batch;

import java.util.*;

/**
 * Enumeration of all possible job states in the migration lifecycle.
 * State transitions follow a strict order defined in JobStateMachine.
 */
public enum JobState {

    PENDING("Pending", false),
    VALIDATING("Validating", true),
    VALIDATION_COMPLETE("Validation Complete", false),
    MAPPING("Mapping", true),
    MAPPING_COMPLETE("Mapping Complete", false),
    IMPORTING("Importing", true),
    INDEXING("Indexing", true),
    COMPLETED("Completed", false),
    FAILED("Failed", false),
    CANCELLED("Cancelled", false);

    private final String displayName;
    private final boolean active;

    JobState(String displayName, boolean active) {
        this.displayName = displayName;
        this.active = active;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return active;
    }

    public Set<JobState> getValidNextStates() {
        return switch (this) {
            case PENDING -> Set.of(VALIDATING);
            case VALIDATING -> Set.of(VALIDATION_COMPLETE, FAILED, CANCELLED);
            case VALIDATION_COMPLETE -> Set.of(MAPPING, IMPORTING, FAILED, CANCELLED);
            case MAPPING -> Set.of(MAPPING_COMPLETE, FAILED, CANCELLED);
            case MAPPING_COMPLETE -> Set.of(IMPORTING, INDEXING, FAILED, CANCELLED);
            case IMPORTING -> Set.of(INDEXING, COMPLETED, FAILED, CANCELLED);
            case INDEXING -> Set.of(COMPLETED, FAILED, CANCELLED);
            case COMPLETED, FAILED, CANCELLED -> Collections.emptySet();
        };
    }

    public boolean canTransitionTo(JobState target) {
        return getValidNextStates().contains(target);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public JobStateCategory getCategory() {
        return switch (this) {
            case PENDING -> JobStateCategory.QUEUED;
            case VALIDATING, MAPPING, IMPORTING, INDEXING -> JobStateCategory.PROCESSING;
            case VALIDATION_COMPLETE, MAPPING_COMPLETE -> JobStateCategory.PROCESSING;
            case COMPLETED -> JobStateCategory.COMPLETED;
            case FAILED, CANCELLED -> JobStateCategory.TERMINAL;
        };
    }

    public enum JobStateCategory {
        QUEUED, PROCESSING, COMPLETED, TERMINAL
    }
}
