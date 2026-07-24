package com.jira.report.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "time_tracking_reports", schema = "jira_report",
    indexes = {
        @Index(name = "idx_time_report_project_id", columnList = "project_id"),
        @Index(name = "idx_time_report_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeTrackingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "issue_id")
    private UUID issueId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_time_seconds")
    @Builder.Default
    private Long totalTimeSeconds = 0L;

    @Column(columnDefinition = "TEXT")
    private String worklogDetails; // JSON array of worklog entries

    @Column(columnDefinition = "TEXT")
    private String breakdown; // JSON breakdown by issue/project

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "report_type", length = 50)
    @Builder.Default
    private String reportType = "USER"; // USER, PROJECT, ISSUE, SPRINT
}