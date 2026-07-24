package com.jira.issue.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TestSet - Group of tests organized for a release, sprint, or feature
 * Tests belong to a test set via test_set_id on the Issue table
 */
@Entity
@Table(name = "test_sets", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_ts_project", columnList = "project_id"),
        @Index(name = "idx_ts_folder", columnList = "folder_id"),
        @Index(name = "idx_ts_status", columnList = "status")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "folder_id")
    private UUID folderId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "test_type", length = 50)
    @Builder.Default
    private String testType = "MANUAL"; // MANUAL, AUTOMATED, MIXED, BDD

    @Column(columnDefinition = "text[]")
    @Builder.Default
    private String[] labels = new String[]{};

    @Column(name = "test_count")
    @Builder.Default
    private Integer testCount = 0;

    @Column(name = "requirement_keys", columnDefinition = "text[]")
    private String[] requirementKeys;

    @Column(length = 30)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, READY, ACTIVE, COMPLETED

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column
    @Builder.Default
    private Boolean archived = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}