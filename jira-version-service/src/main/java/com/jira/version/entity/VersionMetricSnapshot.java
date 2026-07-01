package com.jira.version.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "version_metric_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_issues")
    private Integer totalIssues = 0;

    @Column(name = "open_issues")
    private Integer openIssues = 0;

    @Column(name = "closed_issues")
    private Integer closedIssues = 0;

    @Column(name = "resolved_issues")
    private Integer resolvedIssues = 0;

    @Column(name = "progress_percentage", precision = 5, scale = 2)
    private BigDecimal progressPercentage = BigDecimal.ZERO;

    @Column(name = "total_story_points", precision = 10, scale = 2)
    private BigDecimal totalStoryPoints = BigDecimal.ZERO;

    @Column(name = "completed_story_points", precision = 10, scale = 2)
    private BigDecimal completedStoryPoints = BigDecimal.ZERO;

    @Column(name = "velocity_points", precision = 10, scale = 2)
    private BigDecimal velocityPoints = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}