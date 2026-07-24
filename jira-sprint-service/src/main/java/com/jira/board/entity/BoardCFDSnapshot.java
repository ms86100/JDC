package com.jira.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "board_cfd_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "snapshot_date", "column_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardCFDSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "board_id", nullable = false) private UUID boardId;
    @Column(name = "snapshot_date", nullable = false) private LocalDate snapshotDate;
    @Column(name = "column_id") private UUID columnId;
    @Column(name = "column_name", length = 100) private String columnName;
    @Column(name = "status_category", length = 30) private String statusCategory;
    @Column(name = "issue_count", nullable = false) @Builder.Default private Integer issueCount = 0;
}
