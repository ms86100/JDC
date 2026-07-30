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
 * StepResult - Per-step execution result with evidence tracking
 */
@Entity
@Table(name = "step_results", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_sr_execution", columnList = "execution_id"),
        @Index(name = "idx_sr_issue", columnList = "issue_id"),
        @Index(name = "idx_sr_status", columnList = "status"),
        @Index(name = "idx_sr_defect", columnList = "defect_key")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_type", nullable = false, length = 20)
    private String stepType; // GIVEN, WHEN, THEN, AND, BUT

    @Column(name = "step_description", nullable = false, columnDefinition = "TEXT")
    private String stepDescription;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    // Execution Result
    @Column(length = 20)
    @Builder.Default
    private String status = "NOT_RUN"; // PASSED, FAILED, BLOCKED, SKIPPED, NOT_RUN

    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;

    // Defect Linkage
    @Column(name = "defect_key", length = 100)
    private String defectKey;

    @Column(name = "defect_severity", length = 20)
    private String defectSeverity; // CRITICAL, MAJOR, MINOR

    // Evidence
    @Column(name = "evidence_ids", columnDefinition = "uuid[]")
    private UUID[] evidenceIds;

    @Column(name = "evidence_comments", columnDefinition = "text[]")
    private String[] evidenceComments;

    // Timing
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    // Additional Context
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "text[]")
    private String[] screenshots; // Screenshot URLs

    @Column(columnDefinition = "text[]")
    private String[] logs; // Log file URLs

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}