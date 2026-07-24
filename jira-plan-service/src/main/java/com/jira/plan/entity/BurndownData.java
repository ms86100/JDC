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
@Table(name = "burndown_data", schema = "jira_plan")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BurndownData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "data_date", nullable = false)
    private LocalDate dataDate;

    @Column(name = "ideal_remaining_points")
    private Double idealRemainingPoints;

    @Column(name = "actual_remaining_points")
    private Double actualRemainingPoints;

    @Column(name = "ideal_issue_count")
    private Integer idealIssueCount;

    @Column(name = "actual_issue_count")
    private Integer actualIssueCount;

    @Column(name = "committed_points")
    private Integer committedPoints;

    @Column(name = "completed_points")
    private Integer completedPoints;

    @Column(name = "last_processed_date")
    private LocalDateTime lastProcessedDate;
}