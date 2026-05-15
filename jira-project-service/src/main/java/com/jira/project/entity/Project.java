package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "projects", schema = "jira_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_key", nullable = false, unique = true, length = 10)
    private String projectKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "lead_user_id")
    private UUID leadUserId;

    @Column(name = "project_type", nullable = false, length = 20)
    @Builder.Default
    private String projectType = "COMPANY_MANAGED";

    @Column(name = "template_id")
    private UUID templateId;

    @Column(length = 50)
    private String category;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "default_assignee_type", length = 20)
    @Builder.Default
    private String defaultAssigneeType = "PROJECT_LEAD";

    @Column(name = "allow_issue_creation")
    @Builder.Default
    private Boolean allowIssueCreation = true;

    @Column
    @Builder.Default
    private Boolean archived = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_scheme_id")
    private PermissionScheme permissionScheme;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}