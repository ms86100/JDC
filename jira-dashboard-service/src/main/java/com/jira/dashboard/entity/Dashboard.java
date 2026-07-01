package com.jira.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dashboards", schema = "jira_dashboard",
    indexes = {
        @Index(name = "idx_dashboard_owner_id", columnList = "owner_id"),
        @Index(name = "idx_dashboard_project_id", columnList = "project_id"),
        @Index(name = "idx_dashboard_shared", columnList = "is_shared")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private Boolean isShared = false;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    @Column(name = "layout", length = 50)
    @Builder.Default
    private String layout = "DEFAULT"; // DEFAULT, CUSTOM, TABULAR

    @Column(name = "permission_level", length = 50)
    @Builder.Default
    private String permissionLevel = "PRIVATE"; // PRIVATE, VIEW, EDIT

    @Column(name = "share_permission_type", length = 50)
    @Builder.Default
    private String sharePermissionType = "PRIVATE"; // PRIVATE, GROUP, PROJECT, PUBLIC

    @Column(name = "popularity", nullable = false)
    @Builder.Default
    private Integer popularity = 0;

    @Column(name = "ordering", nullable = false)
    @Builder.Default
    private Integer ordering = 0;

    @Column(columnDefinition = "TEXT")
    private String config; // JSON configuration for dashboard layout

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}