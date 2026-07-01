package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "issue_transition_history", schema = "jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueTransitionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "transition_id")
    private UUID transitionId;

    @Column(name = "transition_name")
    private String transitionName;

    @Column(name = "from_status_id")
    private UUID fromStatusId;

    @Column(name = "to_status_id")
    private UUID toStatusId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "executed_at", nullable = false)
    private OffsetDateTime executedAt;
}
