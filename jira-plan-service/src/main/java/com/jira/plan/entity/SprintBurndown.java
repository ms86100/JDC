package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sprint burndown snapshot (daily data points for burndown chart).
 */
@Entity
@Table(name = "sprint_burndown", schema = "jira_plan", indexes = {
    @Index(name = "idx_sprint_burndown_sprint", columnList = "sprint_id"),
    @Index(name = "idx_sprint_burndown_date", columnList = "snapshot_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintBurndown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_issues", nullable = false)
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "completed_issues", nullable = false)
    @Builder.Default
    private Integer completedIssues = 0;

    @Column(name = "remaining_points", nullable = false)
    @Builder.Default
    private Integer remainingPoints = 0;

    @Column(name = "ideal_remaining", nullable = false)
    @Builder.Default
    private Integer idealRemaining = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}