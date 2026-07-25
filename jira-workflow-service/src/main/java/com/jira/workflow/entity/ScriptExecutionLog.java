package com.jira.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "script_execution_log", schema = "jira_workflow")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "script_id")
    private UUID scriptId;

    @Column(name = "script_key", nullable = false)
    private String scriptKey;

    @Column(name = "script_type", nullable = false, length = 20)
    private String scriptType;

    @Column(name = "execution_mode", nullable = false, length = 20)
    @Builder.Default
    private String executionMode = "WORKFLOW";

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "transition_id")
    private UUID transitionId;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "result_value", columnDefinition = "TEXT")
    private String resultValue;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_ms", nullable = false)
    @Builder.Default
    private Long executionMs = 0L;

    @Column(name = "context_summary", columnDefinition = "TEXT")
    private String contextSummary;

    @Column(name = "executed_by")
    private UUID executedBy;

    @Column(name = "target_issue_id")
    private UUID targetIssueId;

    @Column(name = "api_call_count")
    @Builder.Default
    private Integer apiCallCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
