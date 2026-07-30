package com.avionics_systems.issue.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Execution Completed Event
 * Phase 15 - Event-Driven Architecture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCompletedEvent {
    private UUID eventId;
    private UUID executionId;
    private UUID testId;
    private String testName;
    private String status;
    private int totalSteps;
    private int passedSteps;
    private int failedSteps;
    private int blockedSteps;
    private int skippedSteps;
    private long durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}