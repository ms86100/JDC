package com.avionics_systems.report.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_reports", schema = "jira_report",
    indexes = {
        @Index(name = "idx_project_report_project_id", columnList = "project_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "project_key", length = 20)
    private String projectKey;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "total_issues")
    @Builder.Default
    private Integer totalIssues = 0;

    @Column(name = "open_issues")
    @Builder.Default
    private Integer openIssues = 0;

    @Column(name = "resolved_issues")
    @Builder.Default
    private Integer resolvedIssues = 0;

    @Column(name = "total_story_points")
    @Builder.Default
    private Double totalStoryPoints = 0.0;

    @Column(name = "completed_story_points")
    @Builder.Default
    private Double completedStoryPoints = 0.0;

    @Column(name = "velocity")
    @Builder.Default
    private Double velocity = 0.0;

    @Column(columnDefinition = "TEXT")
    private String issuesByType; // JSON breakdown by issue type

    @Column(columnDefinition = "TEXT")
    private String issuesByStatus; // JSON breakdown by status

    @Column(columnDefinition = "TEXT")
    private String issuesByPriority; // JSON breakdown by priority

    @Column(columnDefinition = "TEXT")
    private String recentActivity; // JSON array of recent activity

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "report_type", length = 50)
    @Builder.Default
    private String reportType = "SUMMARY"; // SUMMARY, DETAILED, TREND, DISTRIBUTION
}