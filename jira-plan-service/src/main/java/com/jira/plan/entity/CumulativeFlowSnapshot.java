package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cumulative_flow_snapshots", schema = "jira_plan",
    uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "snapshot_date", "column_name"}),
    indexes = {
        @Index(name = "idx_cfs_board_date", columnList = "board_id, snapshot_date")
    })
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CumulativeFlowSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "column_name", nullable = false, length = 100)
    private String columnName;

    @Column(name = "issue_count")
    @Builder.Default
    private Integer issueCount = 0;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
