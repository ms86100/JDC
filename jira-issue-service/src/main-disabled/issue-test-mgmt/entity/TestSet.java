package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestSet - Group of related test cases
 */
@Entity
@Table(name = "test_sets", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_ts_project", columnList = "project_id"),
        @Index(name = "idx_ts_folder", columnList = "folder_id"),
        @Index(name = "idx_ts_status", columnList = "status"),
        @Index(name = "idx_ts_type", columnList = "test_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(length = 50)
    @Builder.Default
    private String testType = "MANUAL"; // MANUAL, AUTOMATED, HYBRID

    @Column(length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, ACTIVE, ARCHIVED

    @Column(columnDefinition = "text[]")
    private String[] labels;

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}