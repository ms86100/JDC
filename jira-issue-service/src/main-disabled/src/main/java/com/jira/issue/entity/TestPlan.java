package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestPlan - Container for organizing test set executions with schedule and tracking
 */
@Entity
@Table(name = "test_plans", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_tp_project", columnList = "project_id"),
        @Index(name = "idx_tp_status", columnList = "status"),
        @Index(name = "idx_tp_dates", columnList = "start_date, end_date")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_type", length = 50)
    @Builder.Default
    private String testType = "MANUAL";

    @Column(columnDefinition = "text[]")
    @Builder.Default
    private String[] labels = new String[]{};

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN"; // OPEN, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "target_version", length = 100)
    private String targetVersion;

    @Column(name = "environment", length = 50)
    private String environment; // DEV, STAGING, PROD, CUSTOM

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}