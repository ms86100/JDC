package com.jira.project.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template Workflow Status - Defines the statuses available in a template workflow
 */
@Entity
@Table(name = "template_workflow_statuses", schema = "jira_project")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateWorkflowStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "status_name", nullable = false, length = 50)
    private String statusName;

    @Column(name = "status_key", nullable = false, length = 20)
    private String statusKey;

    @Column(name = "status_color", nullable = false, length = 7)
    @Builder.Default
    private String statusColor = "#6B778C";

    @Column(name = "status_category", nullable = false, length = 20)
    private String statusCategory;

    @Column(nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String icon;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Status categories
    public static final String CATEGORY_TODO = "TODO";
    public static final String CATEGORY_IN_PROGRESS = "IN_PROGRESS";
    public static final String CATEGORY_DONE = "DONE";
}