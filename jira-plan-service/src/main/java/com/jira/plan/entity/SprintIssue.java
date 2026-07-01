package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sprint issue linking (issues assigned to a sprint).
 */
@Entity
@Table(name = "sprint_issues", schema = "jira_plan", indexes = {
    @Index(name = "idx_sprint_issues_sprint", columnList = "sprint_id"),
    @Index(name = "idx_sprint_issues_issue", columnList = "issue_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_item_id")
    private PlanItem planItem;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "rank_value", length = 255)
    private String rankValue;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "added_by")
    private UUID addedBy;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "removed_by")
    private UUID removedBy;

    @Column(name = "completion_status", length = 50)
    @Builder.Default
    private String completionStatus = "UNCOMPLETED";  // UNCOMPLETED, COMPLETED, DROPPED

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "flagged")
    @Builder.Default
    private Boolean flagged = false;

    @Column(name = "flag_reason", length = 500)
    private String flagReason;

    public void remove(UUID removedBy) {
        this.removedAt = LocalDateTime.now();
        this.removedBy = removedBy;
    }

    public void complete() {
        this.completionStatus = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void drop() {
        this.completionStatus = "DROPPED";
        this.removedAt = LocalDateTime.now();
    }
}