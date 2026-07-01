package com.jira.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "test_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to test issue being run
    @Column(name = "test_id", nullable = false)
    private UUID testId;

    // Reference to test execution (parent)
    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(length = 100)
    @Builder.Default
    private String status = "PENDING"; // PENDING, IN_PROGRESS, PASSED, FAILED, BLOCKED, SKIPPED

    @Column(name = "executed_by")
    private UUID executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column
    private Integer duration; // in seconds

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "defect_keys", columnDefinition = "TEXT")
    private String defectKeys; // comma-separated Jira issue keys

    // Step results for this run
    @Column(name = "step_statuses", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> stepStatuses = List.of(); // Array of PASSED/FAILED/BLOCKED per step

    @Column(name = "passed_steps")
    @Builder.Default
    private Integer passedSteps = 0;

    @Column(name = "failed_steps")
    @Builder.Default
    private Integer failedSteps = 0;

    @Column(name = "blocked_steps")
    @Builder.Default
    private Integer blockedSteps = 0;

    @Column(name = "total_steps")
    @Builder.Default
    private Integer totalSteps = 0;

    // Environment info
    @Column(length = 200)
    private String environment;

    @Column(length = 200)
    private String browser;

    @Column(length = 200)
    private String platform;

    @Column(name = "test_data", columnDefinition = "TEXT")
    private String testData;

    // Links to evidence
    @Column(name = "evidence_links", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> evidenceLinks = List.of();

    @Column(columnDefinition = "TEXT")
    private String logs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Whether this run is a retry
    @Column(name = "is_retry")
    @Builder.Default
    private Boolean isRetry = false;

    @Column(name = "parent_run_id")
    private UUID parentRunId;

    // Test run annotations/notes
    @Column(columnDefinition = "TEXT")
    private String annotations;

    // Test run tags for filtering
    @Column(name = "tags", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> tags = List.of();

    // Baseline comparison
    @Column(name = "is_baseline")
    @Builder.Default
    private Boolean isBaseline = false;

    @Column(name = "baseline_id")
    private UUID baselineId;

    // Archival
    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    // Flakiness tracking within this run
    @Column(name = "flakiness_score")
    private Double flakinessScore;

    // Run priority/severity
    @Column(name = "priority", length = 50)
    @Builder.Default
    private String priority = "MEDIUM";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}