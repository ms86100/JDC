package com.jira.component.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "component_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "component_id", nullable = false)
    private UUID componentId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_issues")
    private Integer totalIssues = 0;

    @Column(name = "open_issues")
    private Integer openIssues = 0;

    @Column(name = "closed_issues")
    private Integer closedIssues = 0;

    @Column(name = "bug_count")
    private Integer bugCount = 0;

    @Column(name = "story_count")
    private Integer storyCount = 0;

    @Column(name = "task_count")
    private Integer taskCount = 0;

    @Column(name = "total_story_points", precision = 10, scale = 2)
    private BigDecimal totalStoryPoints = BigDecimal.ZERO;

    @Column(name = "completed_story_points", precision = 10, scale = 2)
    private BigDecimal completedStoryPoints = BigDecimal.ZERO;

    @Column(name = "avg_resolution_time_hours", precision = 10, scale = 2)
    private BigDecimal avgResolutionTimeHours;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}