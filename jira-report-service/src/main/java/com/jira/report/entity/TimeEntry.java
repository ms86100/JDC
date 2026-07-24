package com.jira.report.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "time_entries", schema = "jira_report",
    indexes = {
        @Index(name = "idx_time_entry_issue_id", columnList = "issue_id"),
        @Index(name = "idx_time_entry_user_id", columnList = "user_id"),
        @Index(name = "idx_time_entry_worklog_date", columnList = "worklog_date"),
        @Index(name = "idx_time_entry_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "issue_key", length = 50)
    private String issueKey;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "project_key", length = 10)
    private String projectKey;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "time_spent_seconds", nullable = false)
    @Builder.Default
    private Long timeSpentSeconds = 0L;

    @Column(name = "billable_seconds", nullable = false)
    @Builder.Default
    private Long billableSeconds = 0L;

    @Column(name = "worklog_date", nullable = false)
    private LocalDateTime worklogDate;

    @Column(length = 500)
    private String comment;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "is_billable", nullable = false)
    @Builder.Default
    private Boolean isBillable = false;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}