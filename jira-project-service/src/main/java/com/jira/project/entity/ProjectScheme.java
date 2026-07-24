package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_schemes", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_configuration_scheme_id")
    private FieldConfigurationScheme fieldConfigurationScheme;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}