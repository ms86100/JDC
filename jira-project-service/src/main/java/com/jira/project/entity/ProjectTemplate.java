package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_templates", schema = "jira_project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private ProjectType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon;

    @Column(length = 7)
    private String color;

    @Column(name = "default_assignee_type", length = 20)
    @Builder.Default
    private String defaultAssigneeType = "PROJECT_LEAD";

    @Column(name = "allow_issue_creation")
    @Builder.Default
    private Boolean allowIssueCreation = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 20)
    @Builder.Default
    private String category = "SOFTWARE";

    @Column(name = "template_type", length = 50)
    private String templateType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TemplateCategory templateCategory;

    @Column(name = "workflow_type", length = 50)
    private String workflowType;

    @Column(name = "short_description", length = 255)
    private String shortDescription;

    @Column(name = "icon_emoji", length = 10)
    private String iconEmoji;

    @Column(name = "is_recommended")
    @Builder.Default
    private Boolean isRecommended = false;

    @Column(name = "use_cases", columnDefinition = "TEXT")
    private String useCases;

    @Column(name = "preview_accent", length = 7)
    private String previewAccent;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}