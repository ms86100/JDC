package com.avionics_systems.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log for automation rule executions.
 * Records each time a rule fires, whether it succeeded, and how long it took.
 */
@Entity
@Table(name = "automation_execution_log", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "trigger_issue_id")
    private UUID triggerIssueId;

    @Column(name = "trigger_event", length = 50)
    private String triggerEvent;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    @Column(name = "actions_executed")
    @Builder.Default
    private Integer actionsExecuted = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_duration_ms")
    private Integer executionDurationMs;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    // Execution status constants
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";
}
