package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sprint_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "sprint_id", nullable = false)
    private String sprintId;

    @Column(name = "board_id", nullable = false)
    private String boardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false)
    private SnapshotType snapshotType;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "completed_issues")
    @Builder.Default
    private Integer completedIssues = 0;

    @Column(name = "total_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPoints = BigDecimal.ZERO;

    @Column(name = "completed_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal completedPoints = BigDecimal.ZERO;

    @Column(name = "remaining_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal remainingPoints = BigDecimal.ZERO;

    @Column(name = "original_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal originalPoints = BigDecimal.ZERO;

    @Column(name = "scope_change_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal scopeChangePoints = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SnapshotType {
        COMMITMENT,  // Captured when sprint starts
        DAILY,       // Captured each day for burndown
        CLOSURE      // Captured when sprint closes
    }
}