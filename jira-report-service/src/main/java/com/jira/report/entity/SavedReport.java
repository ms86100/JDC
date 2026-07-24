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
@Table(name = "saved_reports", schema = "jira_report",
    indexes = {
        @Index(name = "idx_saved_report_owner_id", columnList = "owner_id"),
        @Index(name = "idx_saved_report_type", columnList = "report_type")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // TIME_TRACKING, SPRINT, PROJECT

    @Column(columnDefinition = "TEXT")
    private String reportConfig; // JSON configuration for the report

    @Column(columnDefinition = "TEXT")
    private String filters; // JSON filter parameters

    @Column(name = "schedule", length = 100)
    private String schedule; // Cron expression for scheduled reports

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}