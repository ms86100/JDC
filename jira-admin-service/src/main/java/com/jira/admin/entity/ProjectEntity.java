package com.jira.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEntity {

    public enum ProjectType {
        SOFTWARE, BUSINESS, SERVICE_DESK, IT_SERVICE
    }

    public enum ProjectStatus {
        ACTIVE, ARCHIVED, DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String projectKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectType type = ProjectType.SOFTWARE;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Column(name = "lead_user_id")
    private String leadUserId;

    @Column(name = "default_assignee")
    private String defaultAssignee = "unassigned";

    @Column(name = "default_priority")
    private String defaultPriority = "Medium";

    @Column(name = "default_issue_type")
    private String defaultIssueType = "Task";

    @Column(name = "allow_sub_tasks")
    private Boolean allowSubTasks = true;

    @Column(name = "allow_attachments")
    private Boolean allowAttachments = true;

    @Column(name = "allow_comments")
    private Boolean allowComments = true;

    @Column(name = "max_attachments")
    private Integer maxAttachments = 10;

    @Column(name = "workflow_scheme")
    private String workflowScheme;

    @Column(name = "issue_type_scheme")
    private String issueTypeScheme;

    @Column(name = "field_configuration_scheme")
    private String fieldConfigurationScheme;

    @Column(name = "permission_scheme_id")
    private String permissionSchemeId;

    private String projectLevel = "PROJECT";

    @Column(name = "enable_notifications")
    private Boolean enableNotifications = true;

    @Column(name = "notification_events", columnDefinition = "TEXT")
    private String notificationEvents;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "category")
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}