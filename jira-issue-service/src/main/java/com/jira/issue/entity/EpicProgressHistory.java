package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "epic_progress_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpicProgressHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epic_id", nullable = false)
    private String epicId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "total_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPoints = BigDecimal.ZERO;

    @Column(name = "completed_points", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal completedPoints = BigDecimal.ZERO;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "completed_issues")
    @Builder.Default
    private Integer completedIssues = 0;

    @Column(name = "percent_complete", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal percentComplete = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}