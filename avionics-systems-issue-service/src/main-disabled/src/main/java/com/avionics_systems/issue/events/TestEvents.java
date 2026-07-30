package com.avionics_systems.issue.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain Events for Test Management
 * Phase 15 - Event-Driven Architecture
 */
// Note: Public event classes moved to separate files:
// - TestCreatedEvent.java
// - ExecutionStartedEvent.java
// - ExecutionCompletedEvent.java

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TestUpdatedEvent {
    private UUID eventId;
    private UUID testId;
    private String issueKey;
    private UUID projectId;
    private String fieldChanged;
    private Object oldValue;
    private Object newValue;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TestDeletedEvent {
    private UUID eventId;
    private UUID testId;
    private String issueKey;
    private UUID projectId;
    private UUID deletedBy;
    private LocalDateTime deletedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StepResultRecordedEvent {
    private UUID eventId;
    private UUID executionId;
    private UUID testId;
    private int stepIndex;
    private String status;
    private String comment;
    private String defectKey;
    private LocalDateTime recordedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class RequirementLinkedEvent {
    private UUID eventId;
    private String requirementKey;
    private UUID testId;
    private String issueKey;
    private UUID projectId;
    private UUID linkedBy;
    private LocalDateTime linkedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DefectLinkedEvent {
    private UUID eventId;
    private UUID executionId;
    private UUID testId;
    private String defectKey;
    private String status;
    private String comment;
    private LocalDateTime linkedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CucumberImportCompletedEvent {
    private UUID eventId;
    private UUID importBatchId;
    private UUID projectId;
    private String importType;
    private int totalScenarios;
    private int importedScenarios;
    private int failedScenarios;
    private List<String> errors;
    private LocalDateTime completedAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CiCdResultEvent {
    private UUID eventId;
    private String ciSystem;
    private String buildUrl;
    private String branch;
    private String commitSha;
    private int totalTests;
    private int passedTests;
    private int failedTests;
    private int skippedTests;
    private LocalDateTime eventTime;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TestResult {
    private String testName;
    private String className;
    private String status;
    private long duration;
    private String errorMessage;
}