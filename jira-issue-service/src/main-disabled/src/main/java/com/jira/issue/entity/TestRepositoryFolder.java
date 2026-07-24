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
 * TestRepositoryFolder - Hierarchical folder structure for organizing tests
 */
@Entity
@Table(name = "test_repository_folders", schema = "jira_issue",
    indexes = {
        @Index(name = "idx_trf_project", columnList = "project_id"),
        @Index(name = "idx_trf_parent", columnList = "parent_folder_id"),
        @Index(name = "idx_trf_path", columnList = "path")
    }
)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRepositoryFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "parent_folder_id")
    private UUID parentFolderId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String path; // Full path like "/Folder1/Folder2/SubFolder"

    @Column
    @Builder.Default
    private Integer depth = 0;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_smart_folder")
    @Builder.Default
    private Boolean isSmartFolder = false;

    @Column(name = "smart_folder_query", columnDefinition = "TEXT")
    private String smartFolderQuery; // JQL query for smart folders

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}