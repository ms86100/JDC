package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Board column configuration.
 */
@Entity
@Table(name = "board_columns", schema = "jira_plan", indexes = {
    @Index(name = "idx_board_columns_board", columnList = "board_id"),
    @Index(name = "idx_board_columns_sequence", columnList = "board_id, sequence")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardConfig boardConfig;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer sequence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_mapping", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> statusMapping = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "label_values", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> labelValues = new ArrayList<>();

    @Column(name = "min_width")
    @Builder.Default
    private Integer minWidth = 100;

    @Column(name = "max_width")
    @Builder.Default
    private Integer maxWidth = 600;

    @Column(length = 7)
    private String color;  // Hex color

    @Column(name = "max_issues")
    private Integer maxIssues;  // WIP limit

    @Column(name = "constraint_status", length = 50)
    private String constraintStatus;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}