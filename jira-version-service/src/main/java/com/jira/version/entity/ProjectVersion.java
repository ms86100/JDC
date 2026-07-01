package com.jira.version.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "released", nullable = false)
    private Boolean released = false;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "sequence", nullable = false)
    private Integer sequence = 0;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(name = "actual_release_date")
    private LocalDateTime actualReleaseDate;

    @Column(name = "semantic_version", length = 50)
    private String semanticVersion;

    @Column(name = "build_number", length = 100)
    private String buildNumber;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "release_train", length = 100)
    private String releaseTrain;

    @Column(name = "deployment_status", length = 50)
    private String deploymentStatus = "PLANNED";

    @Column(name = "release_status", length = 50)
    private String releaseStatus = "UNRELEASED";

    @Column(name = "release_notes_url", columnDefinition = "TEXT")
    private String releaseNotesUrl;

    @Column(name = "release_notes_generated")
    private Boolean releaseNotesGenerated = false;

    @Column(length = 7)
    private String color;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Column(name = "archived_by")
    private UUID archivedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Computed field helpers
    public Boolean getOverdue() {
        if (releaseDate == null || released) return false;
        return LocalDateTime.now().isAfter(releaseDate);
    }
}