package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "template_scheme_defaults", schema = "jira_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateSchemeDefault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ProjectTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_type_scheme_id")
    private IssueTypeScheme issueTypeScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_scheme_id")
    private WorkflowScheme workflowScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_scheme_id")
    private PermissionScheme permissionScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_scheme_id")
    private NotificationScheme notificationScheme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_scheme_id")
    private ScreenScheme screenScheme;
}