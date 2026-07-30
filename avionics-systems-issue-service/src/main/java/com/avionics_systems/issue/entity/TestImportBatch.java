package com.avionics_systems.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestImportBatch - CI/CD import audit trail
 */
@Entity
@Table(name = "test_import_batches", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_tib_type", columnList = "import_type"),
        @Index(name = "idx_tib_source", columnList = "ci_source"),
        @Index(name = "idx_tib_status", columnList = "status"),
        @Index(name = "idx_tib_created", columnList = "created_at")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "import_type", nullable = false, length = 30)
    private String importType; // JUNIT, CUCUMBER, TESTNG, NUNIT, ROBOT

    @Column(name = "ci_source", length = 100)
    private String ciSource; // JENKINS, GITHUB_ACTIONS, GITLAB_CI, BAMBOO, CIRCLECI, AZURE_DEVOPS

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

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String commitMessage;

    // Import Results
    @Column(name = "total_tests")
    @Builder.Default
    private Integer totalTests = 0;

    @Column(name = "total_passed")
    @Builder.Default
    private Integer totalPassed = 0;

    @Column(name = "total_failed")
    @Builder.Default
    private Integer totalFailed = 0;

    @Column(name = "total_skipped")
    @Builder.Default
    private Integer totalSkipped = 0;

    @Column(name = "tests_created")
    @Builder.Default
    private Integer testsCreated = 0;

    @Column(name = "tests_updated")
    @Builder.Default
    private Integer testsUpdated = 0;

    @Column(name = "executions_created")
    @Builder.Default
    private Integer executionsCreated = 0;

    // Status
    @Column(length = 30)
    @Builder.Default
    private String status = "PROCESSING"; // QUEUED, PROCESSING, COMPLETED, FAILED, PARTIAL

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "text[]")
    private String[] warnings;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}