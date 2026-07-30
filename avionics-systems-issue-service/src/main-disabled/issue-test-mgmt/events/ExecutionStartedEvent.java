package com.avionics_systems.issue.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Execution Started Event
 * Phase 15 - Event-Driven Architecture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStartedEvent {
    private UUID eventId;
    private UUID executionId;
    private UUID testId;
    private String testName;
    private String testEnvironment;
    private UUID executedBy;
    private LocalDateTime startedAt;
}