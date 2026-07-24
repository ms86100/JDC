package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * TestExecutionHistory - Historical tracking of test executions
 */
@Entity
@Table(name = "test_execution_history", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_teh_test", columnList = "test_issue_id"),
        @Index(name = "idx_teh_execution", columnList = "execution_id"),
        @Index(name = "idx_teh_status", columnList = "status"),
        @Index(name = "idx_teh_executed", columnList = "executed_at")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_issue_id", nullable = false)
    private UUID testIssueId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(nullable = false, length = 30)
    private String status; // PASSED, FAILED, BLOCKED, SKIPPED, NOT_RUN

    @Column(name = "executed_by")
    private UUID executedBy;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "test_env", length = 50)
    private String testEnv;

    @Column(name = "duration_ms")
    private Long durationMs;

    // Snapshot of issue state at execution time
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "issue_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> issueSnapshot;
}