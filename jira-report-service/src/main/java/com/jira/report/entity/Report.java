package com.jira.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports", schema = "jira_report",
    indexes = {
        @Index(name = "idx_report_type", columnList = "report_type"),
        @Index(name = "idx_report_project_id", columnList = "project_id"),
        @Index(name = "idx_report_created_by", columnList = "created_by")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType; // TIME_TRACKING, SPRINT, PROJECT, BURNDOWN, VELOCITY

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON configuration for report parameters

    @Column(columnDefinition = "TEXT")
    private String data; // Cached report data in JSON format

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "schedule_cron", length = 100)
    private String scheduleCron; // Cron expression for scheduled reports

    @Column(name = "last_generated_at")
    private LocalDateTime lastGeneratedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}