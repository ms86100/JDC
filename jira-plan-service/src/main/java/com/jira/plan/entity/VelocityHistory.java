package com.jira.plan.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "velocity_history")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "board_id", nullable = false)
    private String boardId;

    @Column(name = "sprint_id")
    private String sprintId;

    @Column(name = "sprint_name")
    private String sprintName;

    @Column(name = "sprint_start_date")
    private LocalDate sprintStartDate;

    @Column(name = "sprint_end_date")
    private LocalDate sprintEndDate;

    @Column(name = "committed_points", precision = 10, scale = 2)
    private BigDecimal committedPoints;

    @Column(name = "completed_points", precision = 10, scale = 2)
    private BigDecimal completedPoints;

    @Column(name = "velocity")
    private Integer velocity;

    @Column(name = "issue_count")
    private Integer issueCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}