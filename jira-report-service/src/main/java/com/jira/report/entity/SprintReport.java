package com.jira.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sprint_reports", schema = "jira_report",
    indexes = {
        @Index(name = "idx_sprint_report_sprint_id", columnList = "sprint_id"),
        @Index(name = "idx_sprint_report_project_id", columnList = "project_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sprint_id", nullable = false)
    private UUID sprintId;

    @Column(name = "sprint_name", nullable = false, length = 255)
    private String sprintName;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "completed_issues")
    @Builder.Default
    private Integer completedIssues = 0;

    @Column(name = "incomplete_issues")
    @Builder.Default
    private Integer incompleteIssues = 0;

    @Column(name = "bugs_count")
    @Builder.Default
    private Integer bugsCount = 0;

    @Column(name = "completion_rate")
    @Builder.Default
    private Double completionRate = 0.0;

    @Column(name = "total_story_points")
    @Builder.Default
    private Double totalStoryPoints = 0.0;

    @Column(name = "completed_story_points")
    @Builder.Default
    private Double completedStoryPoints = 0.0;

    @Column(name = "total_time_seconds")
    @Builder.Default
    private Long totalTimeSeconds = 0L;

    @Column(columnDefinition = "TEXT")
    private String issuesCompleted; // JSON array of completed issue keys

    @Column(columnDefinition = "TEXT")
    private String issuesAddedDuringSprint; // JSON array of added issue keys

    @Column(columnDefinition = "TEXT")
    private String issuesNotCompleted; // JSON array of incomplete issue keys

    @Column(columnDefinition = "TEXT")
    private String issuesLedged; // JSON array of carried over issues

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "burndown_data", columnDefinition = "TEXT")
    private String burndownData; // JSON burndown chart data
}