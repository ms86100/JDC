package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestExecution - Single test run with step-level results
 * Can execute a single test, test set, or entire test plan
 */
@Entity
@Table(name = "test_executions", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_te_project", columnList = "project_id"),
        @Index(name = "idx_te_plan", columnList = "test_plan_id"),
        @Index(name = "idx_te_set", columnList = "test_set_id"),
        @Index(name = "idx_te_status", columnList = "status"),
        @Index(name = "idx_te_tester", columnList = "tester_id"),
        @Index(name = "idx_te_started", columnList = "started_at")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "test_plan_id")
    private UUID testPlanId;

    @Column(name = "test_set_id")
    private UUID testSetId;

    // Can be NULL if running entire test set
    @Column(name = "test_id")
    private UUID testId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 30)
    @Builder.Default
    private String status = "RUNNING"; // RUNNING, PASSED, FAILED, BLOCKED, ABORTED, SKIPPED

    @Column(name = "test_env", length = 50)
    private String testEnv; // DEV, STAGING, PROD, CUSTOM

    @Column(name = "tester_id")
    private UUID testerId;

    @Column(name = "test_cycle", length = 100)
    private String testCycle; // e.g., "Sprint 23", "Release 2.1"

    @Column(name = "sprint_id")
    private UUID sprintId;

    // CI/CD Integration
    @Column(name = "ci_build_url", length = 500)
    private String ciBuildUrl;

    @Column(name = "ci_job_name", length = 255)
    private String ciJobName;

    @Column(name = "ci_build_number", length = 100)
    private String ciBuildNumber;

    @Column(name = "ci_job_id", length = 255)
    private String ciJobId;

    @Column(length = 255)
    private String branch;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    // Test Results Summary
    @Column(name = "total_tests")
    @Builder.Default
    private Integer totalTests = 0;

    @Column(name = "passed_tests")
    @Builder.Default
    private Integer passedTests = 0;

    @Column(name = "failed_tests")
    @Builder.Default
    private Integer failedTests = 0;

    @Column(name = "blocked_tests")
    @Builder.Default
    private Integer blockedTests = 0;

    @Column(name = "skipped_tests")
    @Builder.Default
    private Integer skippedTests = 0;

    @Column(name = "not_run_tests")
    @Builder.Default
    private Integer notRunTests = 0;

    // Timing
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    // Traceability
    @Column(name = "requirement_keys", columnDefinition = "text[]")
    private String[] requirementKeys;

    @Column(name = "defect_keys", columnDefinition = "text[]")
    private String[] defectKeys;

    // Evidence
    @Column(name = "test_report_url", length = 500)
    private String testReportUrl;

    @Column(name = "attachment_ids", columnDefinition = "uuid[]")
    private UUID[] attachmentIds;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public void calculateStatus() {
        if (totalTests == 0) return;
        if (blockedTests > 0 || notRunTests > 0) {
            this.status = "RUNNING";
        } else if (failedTests > 0) {
            this.status = "FAILED";
        } else {
            this.status = "PASSED";
        }
    }

    public double getPassRate() {
        if (totalTests == 0) return 0.0;
        return (double) passedTests / totalTests * 100;
    }
}